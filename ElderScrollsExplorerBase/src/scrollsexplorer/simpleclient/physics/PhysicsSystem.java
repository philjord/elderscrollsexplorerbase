package scrollsexplorer.simpleclient.physics;

import java.util.ArrayList;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.frostwire.util.SparseArray;

import esmj3d.j3d.cell.GridSpace;
import esmj3d.j3d.cell.J3dCELLGeneral;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import esmj3d.physics.PhysicsSystemInterface;
import esmj3d.physics.RayIntersectResult;
import nifbullet.BulletNifModel;
import nifbullet.NavigationProcessorBullet.NbccProvider;
import nifbullet.cha.NBControlledChar;
import nifbullet.cha.NBNonControlledChar;
import nifbullet.dyn.NBSimpleDynamicModel;
import scrollsexplorer.simpleclient.physics.PhysicsDynamics.PhysicsStatus;
import tools.PendingList;
import tools.clock.PeriodicThread;
import tools.clock.PeriodicallyUpdated;
import tools3d.navigation.AvatarCollisionInfo;
import utils.source.MeshSource;

public class PhysicsSystem implements NbccProvider, PhysicsSystemInterface
{
	
	public static int DEBUG_UPDATE_STEP_SKIP = 1; //10 is 1/10 as fast as usual
	
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

	//private PeriodicThread physicsToModelThread;

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
			
			private int debugStepSkipCount = 0;

			@Override
			public void runUpdate()
			{
				if (DEBUG_UPDATE_STEP_SKIP == 1) {
					
					if (System.currentTimeMillis() - startOfLastUpdate > 300)
						System.err
								.println("Physics Sim Thread slow " + (System.currentTimeMillis() - startOfLastUpdate));
					startOfLastUpdate = System.currentTimeMillis();
					
				} else {
					debugStepSkipCount++;
					if (debugStepSkipCount < DEBUG_UPDATE_STEP_SKIP) {
						//TODO: need to allow the char to keep moving really, but that's in physicsTick
						// so all of the debug needs to be in DynamicsEngine really
						return;
					} else {
						debugStepSkipCount = 0;
					}
				}
								
	
				try {
					long startOfTick = System.currentTimeMillis();
					physicsTick();
					// note apply in the same thread, no point in having a running physics the player can't see
					physicsToModelTick();
					if ((System.currentTimeMillis() - startOfTick) > 300)
						System.err.println("physicsTick long " + (System.currentTimeMillis() - startOfTick));
				} catch (Exception e) {
					System.out.println("PhysicsSystem.physicsTick() exception " + e + " " + e.getStackTrace()[0]);
					e.printStackTrace();
				}			
			}
		});
		physicsSimThread.start();
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
		load(cell.getJ3dRECOsById());
	}

	public void unloadJ3dGridSpace(GridSpace cell)
	{
		unload(cell.getJ3dRECOsById());
	}

	public void loadJ3dCELL(J3dCELLGeneral cell)
	{
		//System.out.println("load request for cell " + cell.getName());
		load(cell.getJ3dRECOs());
	}

	public void unloadJ3dCELL(J3dCELLGeneral cell)
	{
		unload(cell.getJ3dRECOs());
	}
	
	private void load(SparseArray<J3dRECOInst> j3dRECOsById)
	{
		//System.out.println("load request for cell " + cell.getName());
		
		// we don't need to pause the physicsLocaleDynamics if we ensure all statics are loaded first, so dynamics have to be "on" something
		// note all dynamics added should start deactivated somehow
		// in other words we want them to "settle" very quickly
		
		// note somewhere around here the 0,0,0 trans needs to be checked for as well
		
		// add the items
		ArrayList<J3dRECOInst> nonDyn = new ArrayList<J3dRECOInst>();
		ArrayList<J3dRECOInst> dyn = new ArrayList<J3dRECOInst>();
		
		for (int i = 0; i < j3dRECOsById.size(); i++)
		{
			J3dRECOInst instReco = j3dRECOsById.get(j3dRECOsById.keyAt(i));
			BulletNifModel bnm = physicsLocaleDynamics.createRECO(instReco);
			if(bnm instanceof NBSimpleDynamicModel || bnm instanceof NBNonControlledChar) {
				dyn.add(instReco);
				
				// FIXME: is this dyn being added with a trans of 0,0,0, if so that's not cool
				bnm.toString();
				
				
			} else {
				nonDyn.add(instReco);		
			}
		}
				
		// use the arraylist of insts are the keys for add, statics first then dyn
		eventsToProcess.add(PhysicsUpdate.createLFM(nonDyn));
		eventsToProcess.add(PhysicsUpdate.createLFMDyn(dyn));
	}

	public void unload(SparseArray<J3dRECOInst> j3dRECOsById)
	{
		ArrayList<J3dRECOInst> nonDyn = new ArrayList<J3dRECOInst>();
		ArrayList<J3dRECOInst> dyn = new ArrayList<J3dRECOInst>();
		
		for (int i = 0; i < j3dRECOsById.size(); i++)
		{
			J3dRECOInst instReco = j3dRECOsById.get(j3dRECOsById.keyAt(i));
			BulletNifModel bnm = physicsLocaleDynamics.getNifBullet(instReco.getRecordId());
			if(bnm instanceof NBSimpleDynamicModel || bnm instanceof NBNonControlledChar)
				dyn.add(instReco);
			else
				nonDyn.add(instReco);
		}
				
		// use the arraylist of insts are the keys for add
		// note dyn first then static
		eventsToProcess.add(PhysicsUpdate.createULFMDyn(dyn));
		eventsToProcess.add(PhysicsUpdate.createULFM(nonDyn));
	}

	public void addRECO(J3dRECOInst j3dRECOInst)
	{
		BulletNifModel bnm = physicsLocaleDynamics.createRECO(j3dRECOInst);
		if(bnm instanceof NBSimpleDynamicModel || bnm instanceof NBNonControlledChar)
			eventsToProcess.add(PhysicsUpdate.createAddDyn(j3dRECOInst));
		else
			System.out.println("Adding single of non dynamics!!");
	}

	public void removeRECO(J3dRECOInst j3dRECOInst)
	{
		BulletNifModel bnm = physicsLocaleDynamics.createRECO(j3dRECOInst);
		if(bnm instanceof NBSimpleDynamicModel || bnm instanceof NBNonControlledChar)
			eventsToProcess.add(PhysicsUpdate.createRemoveDyn(j3dRECOInst));
		else
			System.out.println("Removing single of non dynamics!!");
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
				if (pu.type == PhysicsUpdate.UPDATE_TYPE.LOAD_FROM_MODEL || pu.type == PhysicsUpdate.UPDATE_TYPE.LOAD_FROM_MODEL_DYN)
				{
					//	boolean prevIsPaused = physicsLocaleDynamics.isPaused();
					//	physicsLocaleDynamics.pause();

					// assumes cell id and stmodel set properly by now
					
					for (int i = 0; i < pu.collection.size(); i++)
					{
						J3dRECOInst instReco = pu.collection.get(i);
						physicsLocaleDynamics.addRECO(instReco);
					}

					//	if (!prevIsPaused)
					//		physicsLocaleDynamics.unpause();
				}
				else if (pu.type == PhysicsUpdate.UPDATE_TYPE.UNLOAD_FROM_MODEL || pu.type == PhysicsUpdate.UPDATE_TYPE.UNLOAD_FROM_MODEL_DYN)
				{
					//	boolean prevIsPaused = physicsLocaleDynamics.isPaused();
					//	physicsLocaleDynamics.pause();
					// assumes cell id and stmodel set properly by now
					for (int i = 0; i < pu.collection.size(); i++)
					{
						J3dRECOInst instReco = pu.collection.get(i);
						physicsLocaleDynamics.removeRECO(instReco);
					}
					//	if (!prevIsPaused)
					//		physicsLocaleDynamics.unpause();
				}
				else if (pu.type == PhysicsUpdate.UPDATE_TYPE.ADD_DYN)
				{
					physicsLocaleDynamics.addRECO(pu.reco);
				}
				else if (pu.type == PhysicsUpdate.UPDATE_TYPE.REMOVE_DYN)
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


	private void physicsToModelTick()
	{
		// note this is called on the physics sim thread
		if (!isPaused() && physicsLocaleDynamics != null)
		{
			// always update the character! we want smooth motion
			physicsLocaleDynamics.applyControlledCharacterPhysicsToModel();
			
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

	public ClosestRayResultCallback findRayIntersect(Vector3f rayFrom, Vector3f rayTo, int characterRecordIdToIgnore)
	{
		if (physicsLocaleDynamics != null)
		{
			return physicsLocaleDynamics.findRayIntersect(rayFrom, rayTo, characterRecordIdToIgnore);
		}
		else
		{
			return null;
		}

	}

	@Override
	public RayIntersectResult findRayIntersectResult(Vector3f rayFrom, Vector3f rayTo)
	{
		return findRayIntersectResult(rayFrom, rayTo, -1);
	}

	@Override
	public RayIntersectResult findRayIntersectResult(Vector3f rayFrom, Vector3f rayTo, int characterRecordIdToIgnore)
	{
		RayIntersectResult rayIntersectResult = new RayIntersectResult();
		ClosestRayResultCallback crrc = findRayIntersect(rayFrom, rayTo, characterRecordIdToIgnore);
		if (crrc != null)
		{
			rayIntersectResult.closestHitFraction = crrc.closestHitFraction;
			rayIntersectResult.hitNormalWorld.set(crrc.hitNormalWorld);
			rayIntersectResult.hitPointWorld.set(crrc.hitPointWorld);
			return rayIntersectResult;
		}

		return null;
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

	/**
	 * Dynamic object seperated from stsics as they are added and removed constantly, but must always be added after the static grids in order not to simply
	 * fall away
	 */
	public static class PhysicsUpdate
	{
		public enum UPDATE_TYPE
		{
			//ADD, REMOVE,
			LOAD_FROM_MODEL, UNLOAD_FROM_MODEL, ADD_DYN, REMOVE_DYN, LOAD_FROM_MODEL_DYN, UNLOAD_FROM_MODEL_DYN
		};

		public UPDATE_TYPE type;

		public J3dRECOInst reco = null;

		public ArrayList<J3dRECOInst> collection;

		//possibly these are NEVER appropriate? only single dynamics should be played with?
		// notice kinematics are not dynamics (they alter shape but are rooted down eg doors)
	/*	public static PhysicsUpdate createAdd(J3dRECOInst reco)
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
		}*/

		public static PhysicsUpdate createLFM(ArrayList<J3dRECOInst> collection)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.LOAD_FROM_MODEL;
			pu.collection = collection;
			return pu;
		}

		public static PhysicsUpdate createULFM(ArrayList<J3dRECOInst> collection)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.UNLOAD_FROM_MODEL;
			pu.collection = collection;
			return pu;
		}
		
		public static PhysicsUpdate createAddDyn(J3dRECOInst reco)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.ADD_DYN;
			pu.reco = reco;
			return pu;
		}

		public static PhysicsUpdate createRemoveDyn(J3dRECOInst reco)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.REMOVE_DYN;
			pu.reco = reco;
			return pu;
		}

		public static PhysicsUpdate createLFMDyn(ArrayList<J3dRECOInst> collection)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.LOAD_FROM_MODEL_DYN;
			pu.collection = collection;
			return pu;
		}

		public static PhysicsUpdate createULFMDyn(ArrayList<J3dRECOInst> collection)
		{
			PhysicsUpdate pu = new PhysicsUpdate();
			pu.type = UPDATE_TYPE.UNLOAD_FROM_MODEL_DYN;
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
