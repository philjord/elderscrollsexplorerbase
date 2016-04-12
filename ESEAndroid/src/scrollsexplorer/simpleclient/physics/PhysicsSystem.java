package scrollsexplorer.simpleclient.physics;

import java.util.ArrayList;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.frostwire.util.SparseArray;

import esmj3d.j3d.cell.GridSpace;
import esmj3d.j3d.cell.J3dCELLGeneral;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import nifbullet.BulletNifModel;
import nifbullet.NavigationProcessorBullet.NbccProvider;
import nifbullet.cha.NBControlledChar;
import scrollsexplorer.simpleclient.physics.PhysicsDynamics.PhysicsStatus;
import tools.PendingList;
import tools.clock.PeriodicThread;
import tools.clock.PeriodicallyUpdated;
import tools3d.navigation.AvatarCollisionInfo;
import utils.source.MeshSource;

public class PhysicsSystem implements NbccProvider
{
	public static long MIN_TIME_BETWEEN_STEPS_MS = 20;

	public static Vector3f gravity = new Vector3f(0f, -9.8f, 0f);

	private AvatarCollisionInfo avatarCollisionInfo;

	private BranchGroup behaviourRoot;

	protected InstRECOStore instRECOStore;

	private MeshSource meshSource;

	private long MIN_TIME_BETWEEN_BOUND_UPDATES_MS = 50;

	protected PhysicsDynamics physicsLocaleDynamics;

	private long lastPhysicsBoundUpdate = 0;

	protected int cellId;

	private PeriodicThread physicsSimThread;

	private PeriodicThread physicsToModelThread;

	// Note in one list to ensure time ordering
	private PendingList<PhysicsUpdate> eventsToProcess = new PendingList<PhysicsUpdate>();

	/**
	 * starts paused for loading
	 */

	public PhysicsSystem(InstRECOStore instRECOStore, AvatarCollisionInfo avatarCollisionInfo, BranchGroup behaviourRoot,
			MeshSource meshSource)
	{
		this.avatarCollisionInfo = avatarCollisionInfo;
		this.behaviourRoot = behaviourRoot;
		this.instRECOStore = instRECOStore;
		this.meshSource = meshSource;

		physicsSimThread = new PeriodicThread("Physics Sim Thread", MIN_TIME_BETWEEN_STEPS_MS, new PeriodicallyUpdated() {
			private long startOfLastUpdate = System.currentTimeMillis() * 2;

			public void runUpdate()
			{
				if (System.currentTimeMillis() - startOfLastUpdate > 300)
					System.err.println("PeriodicThread slow " + (System.currentTimeMillis() - startOfLastUpdate));
				startOfLastUpdate = System.currentTimeMillis();

				try
				{
					long startOfTick = System.currentTimeMillis();
					physicsTick();
					if ((System.currentTimeMillis() - startOfTick) > 300)
						System.err.println("physicsTick long " + (System.currentTimeMillis() - startOfTick));
				}
				catch (Exception e)
				{
					System.out.println("PhysicsSystem.physicsTick() exception " + e + " " + e.getStackTrace()[0]);
					e.printStackTrace();
				}
			}
		});
		physicsSimThread.start();

		physicsToModelThread = new PeriodicThread("Physics To Model Thread", MIN_TIME_BETWEEN_STEPS_MS, new PeriodicallyUpdated() {
			public void runUpdate()
			{
				try
				{
					physicsToModelTick();
				}
				catch (Exception e)
				{
					System.out.println("PhysicsSystem.physicsTick() exception " + e + " " + e.getStackTrace()[0]);
					e.printStackTrace();
				}
			}
		});
		physicsToModelThread.start();

	}

	/**
	 * Use -1 cell id to indicate just an unload
	 * @param cid
	 * @param j3dCELLTemporary 
	 * @param instRecoToNif
	 */
	public void cellChanged(int cid, J3dCELLGeneral cell)
	{
		unload();

		this.cellId = cid;

		if (cell != null && cid != -1)
		{
			physicsLocaleDynamics = new PhysicsDynamics(instRECOStore, gravity, behaviourRoot, avatarCollisionInfo, meshSource);
			loadJ3dCELL(cell);
			System.out.println("Physics objects loaded for cell " + cellId);
		}
	}

	@Override
	public NBControlledChar getNBControlledChar()
	{
		if (physicsLocaleDynamics != null)
		{
			return physicsLocaleDynamics.getMyNifBulletChar();
		}
		return null;
	}

	public void loadJ3dGridSpace(GridSpace cell)
	{
		//System.out.println("load request for GridSpace " + cell.getName() + " recos count = " + cell.getJ3dRECOsById().values().size());

		// add the items
		SparseArray<J3dRECOInst> j3dRECOsById = cell.getJ3dRECOsById();
		for (int i = 0; i < j3dRECOsById.size(); i++)
		{
			J3dRECOInst instReco = j3dRECOsById.get(j3dRECOsById.keyAt(i));
			physicsLocaleDynamics.createRECO(instReco);
		}
		// use the arraylist of insts are the keys for add
		eventsToProcess.add(PhysicsUpdate.createLFM(j3dRECOsById));
	}

	public void unloadJ3dGridSpace(GridSpace cell)
	{
		SparseArray<J3dRECOInst> j3dRECOsById = cell.getJ3dRECOsById();
		// removes just need the keys to remove
		eventsToProcess.add(PhysicsUpdate.createULFM(j3dRECOsById));
	}

	public void loadJ3dCELL(J3dCELLGeneral cell)
	{
		//System.out.println("load request for cell " + cell.getName());
		// add the items
		SparseArray<J3dRECOInst> j3dRECOs = cell.getJ3dRECOs();
		for (int i = 0; i < j3dRECOs.size(); i++)
		{
			J3dRECOInst instReco = j3dRECOs.get(j3dRECOs.keyAt(i));
			physicsLocaleDynamics.createRECO(instReco);
		}
		// use the arraylist of insts are the keys for add
		eventsToProcess.add(PhysicsUpdate.createLFM(j3dRECOs));
	}

	public void unloadJ3dCELL(J3dCELLGeneral cell)
	{
		SparseArray<J3dRECOInst> j3dRECOs = cell.getJ3dRECOs();
		// removes just need the keys to remove
		eventsToProcess.add(PhysicsUpdate.createULFM(j3dRECOs));
	}

	public void addRECO(J3dRECOInst j3dRECOInst)
	{
		physicsLocaleDynamics.createRECO(j3dRECOInst);
		eventsToProcess.add(PhysicsUpdate.createAdd(j3dRECOInst));
	}

	public void removeRECO(J3dRECOInst j3dRECOInst)
	{
		eventsToProcess.add(PhysicsUpdate.createRemove(j3dRECOInst));
	}

	protected void unload()
	{
		if (physicsLocaleDynamics != null)
		{
			physicsLocaleDynamics.pause();
			physicsLocaleDynamics.destroy();
		}
		this.cellId = -1;
	}

	protected void setMinTimeForBoundUpdate(long newTime)
	{
		MIN_TIME_BETWEEN_BOUND_UPDATES_MS = newTime;
	}

	public void pause()
	{
		if (physicsLocaleDynamics != null)
		{
			physicsLocaleDynamics.pause();
		}
	}

	public void unpause()
	{
		if (physicsLocaleDynamics != null)
		{
			physicsLocaleDynamics.unpause();
		}
	}

	public boolean isPaused()
	{
		return physicsLocaleDynamics == null || physicsLocaleDynamics.isPaused();
	}

	public void destroy()
	{
		unload();
		System.out.println("Physics stopped");
	}

	private void physicsTick()
	{
		if (physicsLocaleDynamics != null)
		{
			long startOfEventProcessing = System.currentTimeMillis();
			// notice no pause check so we can load up while paused
			ArrayList<PhysicsUpdate> cpl = eventsToProcess.getCurrentPendingList();
			for (PhysicsUpdate pu : cpl)
			{
				if (pu.type == PhysicsUpdate.UPDATE_TYPE.LOAD_FROM_MODEL)
				{
					//	boolean prevIsPaused = physicsLocaleDynamics.isPaused();
					//	physicsLocaleDynamics.pause();

					// assumes cell id and stmodel set properly by now
					for (int i = 0; i < pu.collection.size(); i++)
					{
						J3dRECOInst instReco = pu.collection.get(pu.collection.keyAt(i));					 
						physicsLocaleDynamics.addRECO(instReco);
					}

					//	if (!prevIsPaused)
					//		physicsLocaleDynamics.unpause();
				}
				else if (pu.type == PhysicsUpdate.UPDATE_TYPE.UNLOAD_FROM_MODEL)
				{
					//	boolean prevIsPaused = physicsLocaleDynamics.isPaused();
					//	physicsLocaleDynamics.pause();
					// assumes cell id and stmodel set properly by now
					for (int i = 0; i < pu.collection.size(); i++)
					{
						J3dRECOInst instReco = pu.collection.get(pu.collection.keyAt(i));	
						physicsLocaleDynamics.removeRECO(instReco);
					}
					//	if (!prevIsPaused)
					//		physicsLocaleDynamics.unpause();
				}
				else if (pu.type == PhysicsUpdate.UPDATE_TYPE.ADD)
				{
					physicsLocaleDynamics.addRECO(pu.reco);
				}
				else if (pu.type == PhysicsUpdate.UPDATE_TYPE.REMOVE)
				{
					physicsLocaleDynamics.removeRECO(pu.reco);
				}

			}
			cpl.clear();

			if ((System.currentTimeMillis() - startOfEventProcessing) > 100)
				System.out.println("Physics event processing is slow " + (System.currentTimeMillis() - startOfEventProcessing));

			if (!isPaused())
			{
				physicsLocaleDynamics.dynamicsTick();
			}

		}
	}

	/**
	 * Called by the model update thread to update teh model
	 */
	private void physicsToModelTick()
	{
		if (!isPaused() && physicsLocaleDynamics != null)
		{
			// is it time to update the model from the physics
			long elapsedTime = (System.nanoTime() - lastPhysicsBoundUpdate) / 1000000;
			if (elapsedTime > MIN_TIME_BETWEEN_BOUND_UPDATES_MS)
			{
				physicsLocaleDynamics.applyPhysicsToModel();
				lastPhysicsBoundUpdate = System.nanoTime();
			}
		}
	}

	public PhysicsDynamics getPhysicsLocaleDynamics()
	{
		return physicsLocaleDynamics;
	}

	public void setDisplayDebug(boolean displayDebug)
	{
		if (physicsLocaleDynamics != null)
		{
			synchronized (this)
			{
				physicsLocaleDynamics.setDisplayDebug(displayDebug);
			}
		}
	}

	public boolean isDisplayDebug()
	{
		if (physicsLocaleDynamics != null)
		{
			return physicsLocaleDynamics.isDisplayDebug();
		}
		return false;
	}

	public ClosestRayResultCallback findRayIntersect(Vector3f rayFrom, Vector3f rayTo)
	{
		if (physicsLocaleDynamics != null)
		{
			return physicsLocaleDynamics.findRayIntersect(rayFrom, rayTo);
		}
		else
		{
			return null;
		}

	}

	/**
	 * Interface for thing to get physics object out to inspect details
	 * Don't hang onto the pointer (damn you!)
	 * @param recordId
	 * @return
	 */
	public BulletNifModel getNifBullet(int recordId)
	{
		if (physicsLocaleDynamics != null)
		{
			return physicsLocaleDynamics.getNifBullet(recordId);
		}
		return null;
	}

	public int getRecordId(BulletNifModel nifBullet)
	{
		if (physicsLocaleDynamics != null)
		{
			return physicsLocaleDynamics.getRecordId(nifBullet);
		}
		return -1;
	}

	public PhysicsStatus getPhysicsStatus()
	{

		if (physicsLocaleDynamics != null)
		{
			PhysicsStatus ret = physicsLocaleDynamics.getPhysicsStatus();
			//TODO: 10 long array etc
			ret.averageStepTimeMS = -999;
			return ret;
		}
		else
		{
			return null;
		}
	}

	public static class PhysicsUpdate
	{
		public enum UPDATE_TYPE
		{
			ADD, REMOVE, LOAD_FROM_MODEL, UNLOAD_FROM_MODEL
		};

		public UPDATE_TYPE type;

		public J3dRECOInst reco = null;

		public SparseArray<J3dRECOInst> collection;

		public static PhysicsUpdate createAdd(J3dRECOInst reco)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.ADD;
			pu.reco = reco;
			return pu;
		}

		public static PhysicsUpdate createRemove(J3dRECOInst reco)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.REMOVE;
			pu.reco = reco;
			return pu;
		}

		public static PhysicsUpdate createLFM(SparseArray<J3dRECOInst> collection)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.LOAD_FROM_MODEL;
			pu.collection = collection;
			return pu;
		}

		public static PhysicsUpdate createULFM(SparseArray<J3dRECOInst> collection)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.UNLOAD_FROM_MODEL;
			pu.collection = collection;
			return pu;
		}
	}

	/*	@Override
		public int getAverageStepTimeMS()
		{
			if (physicsLocaleDynamics != null)
			{
				return physicsLocaleDynamics.getAverageStepTimeMS();
			}
			return 0;
		}
	
		@Override
		public int getNumCollisionObjects()
		{
			if (physicsLocaleDynamics != null)
			{
				return physicsLocaleDynamics.getNumCollisionObjects();
			}
			return 0;
		}*/
}
