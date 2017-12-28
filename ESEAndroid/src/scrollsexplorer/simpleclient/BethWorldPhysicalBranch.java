package scrollsexplorer.simpleclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import esmio.common.data.record.Record;
import esmio.common.data.record.Subrecord;
import esmj3d.ai.AIActor;
import esmj3d.j3d.BethRenderSettings;
import esmj3d.j3d.cell.GridSpace;
import esmj3d.j3d.cell.J3dCELLGeneral;
import esmj3d.j3d.cell.J3dICELLPersistent;
import esmj3d.j3d.cell.J3dICellFactory;
import esmj3d.j3d.j3drecords.inst.J3dLAND;
import esmj3d.j3d.j3drecords.inst.J3dRECOChaInst;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import javaawt.Point;
import javaawt.Rectangle;
import nifbullet.BulletNifModel;
import nifbullet.cha.NBNonControlledChar;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import tools.QueuingThread;
import tools3d.utils.YawPitch;
import tools3d.utils.scenegraph.LocationUpdateListener;
import tools3d.utils.scenegraph.StructureUpdateBehavior;

/*
 * Notice it NEVER loads far/distant
 */
public class BethWorldPhysicalBranch extends BranchGroup implements LocationUpdateListener
{
	private int worldFormId;

	private boolean isWRLD = true; // false implies interior cell

	private J3dICELLPersistent j3dCELLPersistent;

	private Vector3f lastUpdatedTranslation = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

	private HashMap<Point, J3dCELLGeneral> loadedNears = new HashMap<Point, J3dCELLGeneral>();
	private Set<Point> loadingNears = Collections.synchronizedSet(new HashSet<Point>());

	private QueuingThread updateThread;

	private StructureUpdateBehavior structureUpdateBehavior;

	private J3dICellFactory j3dCellFactory;

	private PhysicsSystem clientPhysicsSystem;

	private BethRenderSettings.UpdateListener listener = new BethRenderSettings.UpdateListener() {
		public void renderSettingsUpdated()
		{
			updateFromCurrent();
		}
	};

	public BethWorldPhysicalBranch(PhysicsSystem clientPhysicsSystem, int worldFormId, J3dICellFactory j3dCellFactory)
	{
		this.setName("BethWorldPhysicalBranch" + worldFormId);
		this.clientPhysicsSystem = clientPhysicsSystem;
		this.worldFormId = worldFormId;
		this.j3dCellFactory = j3dCellFactory;

		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		// set up to listen for changes to the static render settings
		BethRenderSettings.addUpdateListener(listener);

		// load the general children of this wrld space

		isWRLD = j3dCellFactory.isWRLD(worldFormId);
		if (isWRLD)
		{

			j3dCELLPersistent = j3dCellFactory.makeBGWRLDPersistent(worldFormId, true);
			addChild((J3dCELLGeneral) j3dCELLPersistent);
			clientPhysicsSystem.cellChanged(worldFormId, (J3dCELLGeneral) j3dCELLPersistent);
			//this persistent is just a super cluster of vague things, not related to position at all

			QueuingThread.CallBack callBack = new QueuingThread.CallBack() {
				public void run(Object parameter)
				{
					//ensure we are nearby (character hasn't warped)
					//Point3f currentCharPoint = new Point3f(lastUpdatedTranslation.x, 0, lastUpdatedTranslation.z);
					Point3f p = (Point3f) parameter;

					//TODO: see visual branch about this nonsense here
					//if (currentCharPoint.distance(p) < BethRenderSettings.getFarLoadGridCount())
					{
						update(p.x, -p.z);

					}
				}
			};

			updateThread = new QueuingThread(callBack);
			updateThread.setNewestOnly(true);
			updateThread.setName("Obliv Phys update thread");
			updateThread.setDaemon(true);
			updateThread.start();

			structureUpdateBehavior = new StructureUpdateBehavior();
			addChild(structureUpdateBehavior);

		}
		else
		{
			System.out.println("WHATAA!!! why is this " + this + " being used for interior!");
			/*j3dCELLPersistent = j3dCellFactory.makeBGInteriorCELLPersistent(worldFormId, false);
			addChild((J3dCELLGeneral) j3dCELLPersistent);
			clientPhysicsSystem.cellChanged(worldFormId, (J3dCELLGeneral) j3dCELLPersistent);
			
			j3dCELLTemporary = j3dCellFactory.makeBGInteriorCELLTemporary(worldFormId, false);
			addChild(j3dCELLTemporary);
			clientPhysicsSystem.loadJ3dCELL(j3dCELLTemporary);*/

		}
	}

	public void init(Transform3D charLocation)
	{

		if (isWRLD)
		{
			Vector3f v = new Vector3f();
			charLocation.get(v);
			Point3f p = new Point3f(v);
			lastUpdatedTranslation.set(newTranslation);

			//Note not on a seperate thread
			update(p.x, -p.z);
			if (j3dCELLPersistent != null)
			{
				j3dCELLPersistent.getGridSpaces().update(p.x, -p.z, BethWorldVisualBranch.bethLodManager);
			}
		}
	}

	/**
	 *  Note the phys loads nears and fars using only one near dist as far phys is not important
	 * @param charX
	 * @param charY
	 */
	private void update(float charX, float charY)
	{

		long start = System.currentTimeMillis();

		if (j3dCELLPersistent != null)
		{
			Rectangle bounds = BethWorldVisualBranch.bethLodManager.getGridBounds(charX, charY, BethRenderSettings.getNearLoadGridCount());

			// because j3dcellpersistent is in a lower project I have to do this here, bum			
			List<GridSpace> gridsToRemove = j3dCELLPersistent.getGridSpaces().getGridSpacesToRemove(bounds);
			List<GridSpace> gridsToAdd = j3dCELLPersistent.getGridSpaces().getGridSpacesToAdd(bounds);

			//done after gathering the lists above so we now do the grid changes
			j3dCELLPersistent.getGridSpaces().update(charX, charY, BethWorldVisualBranch.bethLodManager);

			for (GridSpace gridSpace : gridsToRemove)
			{
				clientPhysicsSystem.unloadJ3dGridSpace(gridSpace);
			}

			for (GridSpace gridSpace : gridsToAdd)
			{
				clientPhysicsSystem.loadJ3dGridSpace(gridSpace);
			}

		}

		/*int lowX = (int) Math.floor((charX - (J3dLAND.LAND_SIZE * 0.5)) / J3dLAND.LAND_SIZE);
		int lowY = (int) Math.floor((charY - (J3dLAND.LAND_SIZE * 0.5)) / J3dLAND.LAND_SIZE);
		int highX = (int) Math.ceil((charX + (J3dLAND.LAND_SIZE * 0.5)) / J3dLAND.LAND_SIZE);
		int highY = (int) Math.ceil((charY + (J3dLAND.LAND_SIZE * 0.5)) / J3dLAND.LAND_SIZE);*/

		int lowX = (int) Math.floor(charX / J3dLAND.LAND_SIZE) - 1;
		int lowY = (int) Math.floor(charY / J3dLAND.LAND_SIZE) - 1;
		int highX = (int) Math.floor(charX / J3dLAND.LAND_SIZE) + 1;//grids load out toward positive
		int highY = (int) Math.floor(charY / J3dLAND.LAND_SIZE) + 1;

		// lets remove those loaded nears not in the range
		Iterator<Point> keys = loadedNears.keySet().iterator();
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
			synchronized (loadedNears)
			{
				J3dCELLGeneral bg = loadedNears.remove(key);

				if (bg != null)
				{
					structureUpdateBehavior.remove(this, bg);
					clientPhysicsSystem.unloadJ3dCELL(bg);
				}
			}
		}

		ArrayList<Thread> igors = new ArrayList<Thread>();
		for (int x = lowX; x <= highX; x++)
		{
			for (int y = lowY; y <= highY; y++)
			{
				final Point key = new Point(x, y);
				if (!loadedNears.containsKey(key) && !loadingNears.contains(key))
				{
					loadingNears.add(key);
					//let's split -up we can do more damage that way
					Thread t = new Thread() {
						public void run()
						{
							//Persistent are loaded in  the CELL that is makeBGWRLD all xy based persistents are empty
							J3dCELLGeneral j3dCELLTemporary = j3dCellFactory.makeBGWRLDTemporary(worldFormId, key.x, key.y, true);
							synchronized (loadedNears)
							{
								loadedNears.put(key, j3dCELLTemporary);
								if (j3dCELLTemporary != null)
								{
									j3dCELLTemporary.compile();// better to be done not on the j3d thread?
									structureUpdateBehavior.add(BethWorldPhysicalBranch.this, j3dCELLTemporary);
									clientPhysicsSystem.loadJ3dCELL(j3dCELLTemporary);
								}
							}
							loadingNears.remove(key);
						}
					};
					t.setName("phys load " + key);
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
			System.out.println("BethWorldPhysicalBranch.update took " + (System.currentTimeMillis() - start) + "ms");

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
			updateThread.addToQueue(updatePoint);
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
				updateThread.addToQueue(updatePoint);
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

	public J3dRECOInst getJ3dInstRECO(int recordId)
	{
		synchronized (loadedNears)
		{
			for (J3dCELLGeneral cell : loadedNears.values())
			{
				if (cell != null)
				{
					J3dRECOInst jri = cell.getJ3dRECOs().get(recordId);
					if (jri != null)
					{
						return jri;
					}
				}
			}
		}

		//ok try the persistent cell as well, it'll return null if it's really not here
		return j3dCELLPersistent.getGridSpaces().getJ3dInstRECO(recordId);
	}

	public void setLocationForActor(AIActor aiActor, Vector3f location, YawPitch yawPitch)
	{
		J3dRECOInst j3dRECOInst = getJ3dInstRECO(aiActor.getActorFormId());
		if (j3dRECOInst != null)
		{
			Quat4f q = new Quat4f();
			yawPitch.get(q);
			j3dRECOInst.setLocation(new Vector3f(location.x, location.y, location.z), q);
			BulletNifModel nbm = clientPhysicsSystem.getNifBullet(aiActor.getActorFormId());
			if (nbm instanceof NBNonControlledChar)
			{
				NBNonControlledChar ncc = (NBNonControlledChar) nbm;
				ncc.setTransform(q, location);
			}
			else
			{
				System.out.println("setting location for non actor!! " + nbm);
			}
		}

	}

	public J3dRECOChaInst getVisualActor(AIActor aiActor)
	{
		return (J3dRECOChaInst) getJ3dInstRECO(aiActor.getActorFormId());
	}

}
