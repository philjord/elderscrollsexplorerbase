package scrollsexplorer.simpleclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.LinearFog;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import esmio.common.data.record.Record;
import esmio.common.data.record.Subrecord;
import esmj3d.ai.AIActor;
import esmj3d.j3d.BethRenderSettings;
import esmj3d.j3d.cell.Beth32LodManager;
import esmj3d.j3d.cell.Beth32_4LodManager;
import esmj3d.j3d.cell.BethLodManager;
import esmj3d.j3d.cell.BethNoLodManager;
import esmj3d.j3d.cell.GridSpace;
import esmj3d.j3d.cell.J3dCELLGeneral;
import esmj3d.j3d.cell.J3dICELLPersistent;
import esmj3d.j3d.cell.J3dICellFactory;
import esmj3d.j3d.j3drecords.inst.J3dRECOChaInst;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import javaawt.Point;
import javaawt.Rectangle;
import scrollsexplorer.IDashboard;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import tools.QueuingThread;
import tools3d.utils.Utils3D;
import tools3d.utils.YawPitch;
import tools3d.utils.scenegraph.LocationUpdateListener;
import tools3d.utils.scenegraph.StructureUpdateBehavior;

/**
 * 
 * @author phil
 *
 */
public class BethWorldVisualBranch extends BranchGroup implements LocationUpdateListener
{

	//when set true the havok data will only be loaded once for physics
	// and the red lines will not be able to be seen 
	public static boolean LOAD_PHYS_FROM_VIS = false;

	public static Color3f FOG_COLOR = new Color3f(0.8f, 0.8f, 0.8f);

	public static float FOG_START = 100;

	public static float FOG_END = 1000;

	private int worldFormId;

	private J3dICELLPersistent j3dCELLPersistent;

	private boolean isWRLD = true; // false implies interior cell

	private Vector3f lastUpdatedTranslation = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

	private Map<Point, J3dCELLGeneral> loadedNears = Collections.synchronizedMap(new HashMap<Point, J3dCELLGeneral>());

	private Map<Point, J3dCELLGeneral> loadedFars = Collections.synchronizedMap(new HashMap<Point, J3dCELLGeneral>());

	private Set<Point> loadingNears = Collections.synchronizedSet(new HashSet<Point>());

	private Set<Point> loadingFars = Collections.synchronizedSet(new HashSet<Point>());

	private QueuingThread nearUpdateThread;

	private QueuingThread grossUpdateThread;

	private StructureUpdateBehavior structureUpdateBehavior;

	private J3dICellFactory j3dCellFactory;

	private PhysicsSystem clientPhysicsSystem;

	private LinearFog fog = new LinearFog(FOG_COLOR, FOG_START, FOG_END);

	// TODO: on change don't dump gross until we forcable need a different one
	public static BethLodManager bethLodManager;

	private BethRenderSettings.UpdateListener listener = new BethRenderSettings.UpdateListener() {
		@Override
		public void renderSettingsUpdated()
		{
			updateFromCurrent();
		}
	};

	public BethWorldVisualBranch(int worldFormId, J3dICellFactory j3dCellFactory, PhysicsSystem clientPhysicsSystem)
	{
		this.clientPhysicsSystem = clientPhysicsSystem;
		this.setName("BethWorldVisualBranch" + worldFormId);
		this.worldFormId = worldFormId;
		this.j3dCellFactory = j3dCellFactory;

		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		// not no scope is universal so not a system for disabling
		fog.addScope(this);
		fog.setCapability(LinearFog.ALLOW_INFLUENCING_BOUNDS_WRITE);
		if (BethRenderSettings.isFogEnabled())
		{
			fog.setInfluencingBounds(Utils3D.defaultBounds);
		}		
		addChild(fog);

		IDashboard.dashboard.setLodLoading(1);
		//Expensive to load, so keep it around and only change when must
		if (bethLodManager == null)
		{
			//TODO: all these should be connected strongly to GameConfig
			if (j3dCellFactory.getMainESMFileName().indexOf("Morrowind") != -1)
			{
				bethLodManager = new BethNoLodManager(j3dCellFactory);
			}
			else if (j3dCellFactory.getMainESMFileName().indexOf("HunterSneaker") != -1)
			{
				esmj3dtes4.j3d.j3drecords.inst.J3dREFRFactory.NATURAL_ANIMALS_ONLY = true;
			}
			else if (j3dCellFactory.getMainESMFileName().indexOf("Oblivion") != -1)
			{
				bethLodManager = new Beth32LodManager(j3dCellFactory);
			}
			else
			{
				bethLodManager = new Beth32_4LodManager(j3dCellFactory);
			}
		}
		bethLodManager.detach();
		bethLodManager.setWorldFormId(worldFormId);
		addChild(bethLodManager);

		IDashboard.dashboard.setLodLoading(-1);

		// set up to listener for changes to teh static render settings
		BethRenderSettings.addUpdateListener(listener);

		isWRLD = j3dCellFactory.isWRLD(worldFormId);
		if (isWRLD)
		{
			//load the general children of this wrld space
			j3dCELLPersistent = j3dCellFactory.makeBGWRLDPersistent(worldFormId, false);
			addChild((J3dCELLGeneral) j3dCELLPersistent);

			if (BethWorldVisualBranch.LOAD_PHYS_FROM_VIS)
			{
				clientPhysicsSystem.cellChanged(worldFormId, (J3dCELLGeneral) j3dCELLPersistent);
			}

			QueuingThread.CallBack nearCallBack = new QueuingThread.CallBack() {
				@Override
				public void run(Object parameter)
				{
					updateNear((Point3f) parameter);
				}
			};

			nearUpdateThread = new QueuingThread(nearCallBack);
			nearUpdateThread.setNewestOnly(true);
			nearUpdateThread.setName("Beth Vis near update thread");
			nearUpdateThread.setDaemon(true);
			nearUpdateThread.start();

			QueuingThread.CallBack grossCallBack = new QueuingThread.CallBack() {
				@Override
				public void run(Object parameter)
				{
					updateGross((Point3f) parameter);
				}
			};

			grossUpdateThread = new QueuingThread(grossCallBack);
			grossUpdateThread.setNewestOnly(true);
			grossUpdateThread.setName("Beth Vis gross update thread");
			grossUpdateThread.setDaemon(true);
			grossUpdateThread.start();

			//NOTE! j3d does not allow multi threaded access to add and remove groups
			// It can cause deadlocks, betterdistanceLOD on teh behavior thread is
			// doing adds and removes, so these queueing thread need to be on a behavior as well.
			structureUpdateBehavior = new StructureUpdateBehavior();
			structureUpdateBehavior.setMaxElapsedTimeForCalls(20);
			addChild(structureUpdateBehavior);

		}
		else
		{
			System.out.println("WHATAA!!! why is this " + this + " being used for interior!");
		}

	}

	public void unload()
	{
		bethLodManager.detach();
	}

	/**
	 * Note this MUST be called with the visuals not yet attached, it does much structure change
	 * @param charLocation
	 */
	public void init(Transform3D charLocation)
	{
		IDashboard.dashboard.setNearLoading(1);
		Vector3f v = new Vector3f();
		charLocation.get(v);
		Point3f p = new Point3f(v);
		charLocation.get(newTranslation);
		lastUpdatedTranslation.set(newTranslation);

		//Note not on a separate thread		
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().update(p.x, -p.z, bethLodManager);

			if (BethWorldVisualBranch.LOAD_PHYS_FROM_VIS)
			{
				Rectangle bounds = BethWorldVisualBranch.bethLodManager.getGridBounds(p.z, p.z, BethRenderSettings.getNearLoadGridCount());

				// because j3dcellpersistent is in a lower project I have to do this here, bum			
				List<GridSpace> gridsToRemove = j3dCELLPersistent.getGridSpaces().getGridSpacesToRemove(bounds);
				List<GridSpace> gridsToAdd = j3dCELLPersistent.getGridSpaces().getGridSpacesToAdd(bounds);

				for (GridSpace gridSpace : gridsToRemove)
				{
					clientPhysicsSystem.unloadJ3dGridSpace(gridSpace);
				}

				for (GridSpace gridSpace : gridsToAdd)
				{
					clientPhysicsSystem.loadJ3dGridSpace(gridSpace);
				}
			}

		}
		Point3f updatePoint = new Point3f(lastUpdatedTranslation.x, 0, lastUpdatedTranslation.z);
		updateNear(updatePoint);
		updateGross(updatePoint);

		IDashboard.dashboard.setNearLoading(-1);

	}

	private void updateGross(Point3f p)
	{
		if (isWRLD)
		{
			IDashboard.dashboard.setLodLoading(1);
			updateGross(p.x, -p.z);
			IDashboard.dashboard.setLodLoading(-1);
		}
	}

	private static void updateGross(float charX, float charY)
	{
		bethLodManager.updateGross(charX, charY);
	}

	private void updateNear(Point3f p)
	{
		//in case of warp fix up the old but ignore new?
		//Point3f currentCharPoint = new Point3f(lastUpdatedTranslation.x, 0, lastUpdatedTranslation.z);

		//TODO: this is garbage! dist is in meters, grids are in grids, near+far is max view distance, not far alone?
		// what was this trying to do here anyway?
		//	if (currentCharPoint.distance(p) < BethRenderSettings.getFarLoadGridCount())
		{
			IDashboard.dashboard.setNearLoading(1);
			if (j3dCELLPersistent != null)
			{
				j3dCELLPersistent.getGridSpaces().update(p.x, -p.z, bethLodManager);

				if (BethWorldVisualBranch.LOAD_PHYS_FROM_VIS)
				{
					Rectangle bounds = BethWorldVisualBranch.bethLodManager.getGridBounds(p.z, p.z,
							BethRenderSettings.getNearLoadGridCount());

					// because j3dcellpersistent is in a lower project I have to do this here, bum			
					List<GridSpace> gridsToRemove = j3dCELLPersistent.getGridSpaces().getGridSpacesToRemove(bounds);
					List<GridSpace> gridsToAdd = j3dCELLPersistent.getGridSpaces().getGridSpacesToAdd(bounds);

					for (GridSpace gridSpace : gridsToRemove)
					{
						clientPhysicsSystem.unloadJ3dGridSpace(gridSpace);
					}

					for (GridSpace gridSpace : gridsToAdd)
					{
						clientPhysicsSystem.loadJ3dGridSpace(gridSpace);
					}
				}
			}

			updateNear(p.x, -p.z);

		}
	}

	private void updateNear(float charX, float charY)
	{
		Rectangle bounds = bethLodManager.getGridBounds(charX, charY, BethRenderSettings.getNearLoadGridCount());

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

		ArrayList<Thread> igors = new ArrayList<Thread>();
		for (int x = bounds.x; x <= bounds.x + bounds.width; x++)
		{
			for (int y = bounds.y; y <= bounds.y + bounds.height; y++)
			{
				final Point key = new Point(x, y);

				if (!loadedNears.containsKey(key) && !loadingNears.contains(key))
				{
					loadingNears.add(key);
					//Persistent are loaded in  the CELL that is makeBGWRLD all xy based persistents are empty

					//let's split -up we can do more damage that way
					Thread t = new Thread() {
						@Override
						public void run()
						{
							J3dCELLGeneral bg = j3dCellFactory.makeBGWRLDTemporary(worldFormId, key.x, key.y, false);

							loadedNears.put(key, bg);

							//NOTE nears own the detailed land					
							if (bg != null)
							{
								bg.compile();
								structureUpdateBehavior.add(BethWorldVisualBranch.this, bg);

								if (BethWorldVisualBranch.LOAD_PHYS_FROM_VIS)
								{
									clientPhysicsSystem.loadJ3dCELL(bg);
								}

								// now get rid of any fars that have the same keys loaded in
								J3dCELLGeneral bgFar = loadedFars.get(key);
								if (bgFar != null)
								{
									structureUpdateBehavior.remove(BethWorldVisualBranch.this, bgFar);
									loadedFars.remove(key);
								}
							}
							else
							{
								// normally just asking off the edge of map, should be fine
								//System.out.println("bg==null! " +key);
							}

							loadingNears.remove(key);
						}
					};
					t.setName("makeBGWRLDTemporaryNear " + key);
					t.start();
					igors.add(t);
				}

			}
		}
		//now we wait for igors to come back from their missions
		for (Thread t : igors)
		{
			try
			{
				t.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		if ((System.currentTimeMillis() - start) > 50)
			System.out.println("BethWorldVisualBranch.updateNear took " + (System.currentTimeMillis() - start) + "ms");

		//add the fars in before removing the near it will replace
		IDashboard.dashboard.setNearLoading(-1);
		IDashboard.dashboard.setFarLoading(1);
		updateFar(charX, charY, keysToRemove);
		IDashboard.dashboard.setFarLoading(-1);

		for (int i = 0; i < keysToRemove.size(); i++)
		{
			Point key = keysToRemove.get(i);
			BranchGroup bg = loadedNears.get(key);
			if (bg != null)
			{
				structureUpdateBehavior.remove(this, bg);

				if (BethWorldVisualBranch.LOAD_PHYS_FROM_VIS)
				{
					clientPhysicsSystem.unloadJ3dCELL((J3dCELLGeneral) bg);
				}
			}

			loadedNears.remove(key);

		}

	}

	/**
	 * This only does things for Oblivion, tes5 doesn't use it
	 * @param charX
	 * @param charY
	 * @param nearsAboutToBeRemoved 
	 * @param isLive 
	 */
	private void updateFar(float charX, float charY, ArrayList<Point> nearsAboutToBeRemoved)
	{
		long start = System.currentTimeMillis();

		// Note simple system used, as no lands invloved here
		Rectangle bounds = Beth32LodManager.getBounds(charX, charY, BethRenderSettings.getFarLoadGridCount());

		final int lowX = bounds.x;
		final int lowY = bounds.y;
		final int highX = bounds.x + bounds.width;
		final int highY = bounds.y + bounds.height;

		// lets remove those loaded fars not in the range
		Iterator<Point> keys = loadedFars.keySet().iterator();
		ArrayList<Point> keysToRemove = new ArrayList<Point>();
		while (keys.hasNext())
		{
			Point key = keys.next();
			if (key.x < lowX || key.x > highX || key.y < lowY || key.y > highY)
			{
				keysToRemove.add(key);
			}
		}

		for (int i = 0; i < keysToRemove.size(); i++)
		{
			Point key = keysToRemove.get(i);
			BranchGroup bg = loadedFars.get(key);
			if (bg != null)
			{
				structureUpdateBehavior.remove(this, bg);
				loadedFars.remove(key);
			}
		}
		ArrayList<Thread> igors = new ArrayList<Thread>();
		for (int x = lowX; x <= highX; x++)
		{
			for (int y = lowY; y <= highY; y++)
			{
				final Point key = new Point(x, y);

				// don't load if already there or coming
				if (!loadedFars.containsKey(key) && !loadingFars.contains(key)
				// don't load fars where a near is (but do where a near is about to leave	)				
						&& (nearsAboutToBeRemoved.contains(key) || !loadedNears.containsKey(key)))
				{
					loadingFars.add(key);
					//long start = System.currentTimeMillis();
					Thread t = new Thread() {
						@Override
						public void run()
						{
							//System.out.println("updateFar3 " + key);
							J3dCELLGeneral bg = j3dCellFactory.makeBGWRLDDistant(worldFormId, key.x, key.y, false);
							loadedFars.put(key, bg);
							if (bg != null)
							{
								bg.compile();// better to be done not on the j3d thread?
								structureUpdateBehavior.add(BethWorldVisualBranch.this, bg);
								//System.out.println("updateFar3 " + key + " " + (System.currentTimeMillis() - start));
							}
							loadingFars.remove(key);
						}
					};
					t.setName("makeBGWRLDTemporaryFar " + key);
					t.start();
					igors.add(t);
				}
			}
		}

		for (Thread t : igors)
		{
			try
			{
				t.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		if ((System.currentTimeMillis() - start) > 50)
			System.out.println("BethWorldVisualBranch.updateFar took " + (System.currentTimeMillis() - start) + "ms");
	}

	/**
	 * Called when the static render settings are changed
	 *
	 */
	public void updateFromCurrent()
	{
		if (isWRLD)
		{
			Point3f updatePoint = new Point3f(lastUpdatedTranslation.x, 0, lastUpdatedTranslation.z);
			nearUpdateThread.addToQueue(updatePoint);
			grossUpdateThread.addToQueue(updatePoint);

			if (BethRenderSettings.isFogEnabled())
			{

				fog.setInfluencingBounds(Utils3D.defaultBounds);
			}
			else
			{
				fog.setInfluencingBounds(null);
			}
		}
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
				grossUpdateThread.addToQueue(updatePoint);
			}
		}
	}

	public void handleRecordCreate(Record record)
	{
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().handleRecordCreate(record);
		}
	}

	public void handleRecordDelete(Record record)
	{
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().handleRecordDelete(record);
		}
	}

	public void handleRecordUpdate(Record record, Subrecord updatedSubrecord)
	{
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().handleRecordUpdate(record, updatedSubrecord);
		}

	}

	public J3dRECOInst getJ3dInstRECO(int recoId)
	{
		synchronized (loadedNears)
		{
			//System.out.println("asked for " +recoId);
			for (J3dCELLGeneral cell : loadedNears.values())
			{
				//FIXME: often this does not work in Morrowind for the mouse over, but it works
				// everytime if visual is shared with physics as if the ids are being not reused

				//customs and excise gates are 49346 (is in visuals) when good but
				//60466 when bad, must be a generating a new id for physics side
				if (cell != null)
				{
					J3dRECOInst jri = cell.getJ3dRECOs().get(recoId);
					if (jri != null)
					{
						return jri;
					}
				}
			}
		}
		//ok try the persistent cell as well, it'll return null if it's really not here
		if (j3dCELLPersistent != null && j3dCELLPersistent.getGridSpaces() != null)
			return j3dCELLPersistent.getGridSpaces().getJ3dInstRECO(recoId);
		else
			return null;
	}

	public void setLocationForActor(AIActor aiActor, Vector3f location, YawPitch yawPitch)
	{
		J3dRECOInst j3dRECOInst = getJ3dInstRECO(aiActor.getActorFormId());
		if (j3dRECOInst != null)
		{
			Quat4f q = new Quat4f();
			yawPitch.get(q);
			j3dRECOInst.setLocation(new Vector3f(location.x, location.y, location.z), q);
		}

	}

	public J3dRECOChaInst getVisualActor(AIActor aiActor)
	{
		return (J3dRECOChaInst) getJ3dInstRECO(aiActor.getActorFormId());
	}

}