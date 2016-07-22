package scrollsexplorer.simpleclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import esmj3d.j3d.BethRenderSettings;
import esmj3d.j3d.cell.AICellGeneral;
import esmj3d.j3d.cell.Beth32LodManager;
import esmj3d.j3d.cell.J3dICellFactory;
import javaawt.Point;
import javaawt.Rectangle;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import tools.QueuingThread;
import tools.clock.PeriodicThread;
import tools.clock.PeriodicallyUpdated;
import tools3d.utils.scenegraph.LocationUpdateListener;

public class BethAIControl implements LocationUpdateListener
{
	public static long MIN_TIME_THINK_MS = 1000;
	public static long MIN_TIME_ACT_MS = 100;

	private int curentCellId = -1;

	private boolean isWRLD = true; // false implies interior cell

	private Vector3f lastUpdatedTranslation = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

	private Map<Point, AICellGeneral> loadedNears = Collections.synchronizedMap(new HashMap<Point, AICellGeneral>());

	private QueuingThread nearUpdateThread;

	private J3dICellFactory j3dCellFactory;

	private PhysicsSystem clientPhysicsSystem;

	private SimpleWalkSetupInterface simpleWalkSetup;

	private SimpleBethCellManager simpleBethCellManager;

	private PeriodicThread thinkingThread;

	private PeriodicThread actingThread;

	private boolean isPaused = true;

	public BethAIControl(SimpleWalkSetupInterface simpleWalkSetup2, SimpleBethCellManager simpleBethCellManager,
			J3dICellFactory j3dCellFactory, PhysicsSystem clientPhysicsSystem)
	{
		this.simpleWalkSetup = simpleWalkSetup2;
		this.simpleBethCellManager = simpleBethCellManager;
		this.clientPhysicsSystem = clientPhysicsSystem;
		this.j3dCellFactory = j3dCellFactory;

		QueuingThread.CallBack nearCallBack = new QueuingThread.CallBack() {
			@Override
			public void run(Object parameter)
			{
				updateNear((Point3f) parameter);
			}
		};

		nearUpdateThread = new QueuingThread(nearCallBack);
		nearUpdateThread.setNewestOnly(true);
		nearUpdateThread.setName("Beth AI  update thread");
		nearUpdateThread.setDaemon(true);
		nearUpdateThread.start();

		thinkingThread = new PeriodicThread("Thinking  Thread", MIN_TIME_THINK_MS, new PeriodicallyUpdated() {
			@Override
			public void runUpdate()
			{
				doAllThoughts();
			}
		});
		thinkingThread.start();

		actingThread = new PeriodicThread("Acting  Thread", MIN_TIME_ACT_MS, new PeriodicallyUpdated() {
			@Override
			public void runUpdate()
			{
				doAllActions();
			}
		});
		actingThread.start();
	}

	//	deburner
	private Vector3f newTranslation = new Vector3f();

	private Point3f p1 = new Point3f();

	private Point3f p2 = new Point3f();

	@Override
	public void locationUpdated(Quat4f rot, Vector3f trans)
	{
		if (isWRLD)
		{
			newTranslation.set(trans);
			p1.set(newTranslation);
			p2.set(lastUpdatedTranslation);
			if (p1.distance(p2) > 2)
			{
				lastUpdatedTranslation.set(newTranslation);

				Point3f updatePoint = new Point3f(newTranslation.x, 0, newTranslation.z);
				nearUpdateThread.addToQueue(updatePoint);

			}
		}
		else
		{
			// notice in thread
			updateNear(0, 0);
		}
	}

	public void cellChanged(int newCellId, boolean isWRLD)
	{
		this.isWRLD = isWRLD;

		//new cell AI will be loaded on the location update call 
		curentCellId = newCellId;
	}

	public void init(Transform3D charLocation)
	{
		charLocation.get(newTranslation);
		lastUpdatedTranslation.set(newTranslation);

		Point3f updatePoint = new Point3f(lastUpdatedTranslation.x, 0, lastUpdatedTranslation.z);
		updateNear(updatePoint);

		// interior just get loaded now
		if (!isWRLD)
		{
			System.out.println("Interior loaded up?? " + curentCellId);
			// interiors are just a single cell loaded
			AICellGeneral bg = j3dCellFactory.makeAICell(curentCellId, simpleBethCellManager);
			if (bg != null)
			{
				loadedNears.put(new Point(0, 0), bg);
			}
		}

	}

	private void updateNear(Point3f p)
	{
		updateNear(p.x, -p.z);
	}

	private void updateNear(float charX, float charY)
	{
		if (isWRLD)
		{
			Rectangle bounds = Beth32LodManager.getBounds(charX, charY, BethRenderSettings.getNearLoadGridCount());
			long start = System.currentTimeMillis();

			// figure out nears not in the range
			Iterator<Point> keys = loadedNears.keySet().iterator();
			ArrayList<Point> keysToRemove = new ArrayList<Point>();
			while (keys.hasNext())
			{
				Point key = keys.next();
				if (key.x < bounds.x || key.x > bounds.x + bounds.width || key.y < bounds.y || key.y > bounds.y + bounds.height)
				{
					keysToRemove.add(key);
				}
			}

			for (int i = 0; i < keysToRemove.size(); i++)
			{
				Point key = keysToRemove.get(i);
				synchronized (loadedNears)
				{
					AICellGeneral bg = loadedNears.remove(key);
					if (bg != null)
					{
						bg.unloadCell();
					}
				}
			}

			for (int x = bounds.x; x <= bounds.x + bounds.width; x++)
			{
				for (int y = bounds.y; y <= bounds.y + bounds.height; y++)
				{
					final Point key = new Point(x, y);

					if (!loadedNears.containsKey(key))
					{

						AICellGeneral bg = j3dCellFactory.makeAICell(curentCellId, key.x, key.y, simpleBethCellManager);
						//simpleBethCellManager.currentBethInteriorPhysicalBranch

						if (bg != null)
						{
							synchronized (loadedNears)
							{
								loadedNears.put(key, bg);
							}
						}
					}
				}
			}

			if ((System.currentTimeMillis() - start) > 50)
				System.out.println("BethAIControl.makeAICell took " + (System.currentTimeMillis() - start) + "ms");
		}

	}

	public void unload()
	{
		pause();
		synchronized (loadedNears)
		{
			for (AICellGeneral aiCell : loadedNears.values())
			{
				aiCell.unloadCell();
			}
		}
		loadedNears.clear();
	}

	private void doAllThoughts()
	{
		if (!isPaused)
		{

			synchronized (loadedNears)
			{
				for (AICellGeneral aiCell : loadedNears.values())
				{
					aiCell.doAllThoughts(lastUpdatedTranslation, clientPhysicsSystem);
				}
			}
		}

	}

	private void doAllActions()
	{
		if (!isPaused)
		{
			synchronized (loadedNears)
			{
				for (AICellGeneral aiCell : loadedNears.values())
				{
					aiCell.doAllActions(lastUpdatedTranslation, clientPhysicsSystem);
				}
			}
		}
	}

	public void pause()
	{
		isPaused = true;
	}

	public void resume()
	{
		isPaused = false;
	}

}
