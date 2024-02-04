package scrollsexplorer.simpleclient.physics;

import java.util.HashMap;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseProxy;
import com.bulletphysics.collision.broadphase.DbvtProxy;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.bulletphysics.dynamics.RigidBody;
import com.frostwire.util.SparseArray;

import esmj3d.j3d.j3drecords.inst.J3dLAND;
import esmj3d.j3d.j3drecords.inst.J3dRECOChaInst;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import esmj3d.j3d.j3drecords.type.J3dRECOType;
import esmj3dtes3.j3d.j3drecords.type.J3dPivotDOOR;
import nif.NifFile;
import nif.NifToJ3d;
import nif.j3d.animation.J3dNiControllerManager;
import nif.j3d.animation.J3dNiControllerSequence;
import nifbullet.BulletNifModel;
import nifbullet.BulletNifModelClassifier;
import nifbullet.cha.NBControlledChar;
import nifbullet.cha.NBNonControlledChar;
import nifbullet.dyn.NBSimpleDynamicModel;
import nifbullet.simple.NBSimpleModel;
import nifbullet.util.debug.opengl.DebugOutput;
import nifbullet.util.debug.opengl.LWJGL;
import tools3d.navigation.AvatarCollisionInfo;
import tools3d.navigation.AvatarLocation;
import tools3d.utils.Utils3D;
import tools3d.utils.scenegraph.StructureUpdateBehavior;
import utils.source.MeshSource;

public class PhysicsDynamics extends DynamicsEngine
{
	//public static HeadlessUniverse headlessUniverse = new HeadlessUniverse();

	protected InstRECOStore instRecoToNif;

	private SparseArray<NifBulletBinding> instRecoBulletBindings = new SparseArray<NifBulletBinding>();

	private HashMap<BulletNifModel, Integer> nifBulletToRecoId = new HashMap<BulletNifModel, Integer>();

	private SparseArray<BulletNifModel> recoIdToNifBullet = new SparseArray<BulletNifModel>();

	private BranchGroup dynamicsRootBranchGroup;

	private StructureUpdateBehavior structureUpdateBehavior;

	private boolean displayDebug = false;

	private static boolean debugOutputInited = false;

	private AvatarLocation avatarLocation;

	private NBControlledChar myNifBulletChar;

	private ClientNifBulletCharBinding clientNifBulletCharBinding;

	private MeshSource meshSource;

	private boolean destroyed;

	public PhysicsDynamics(InstRECOStore instRecoToNif, Vector3f gravity, BranchGroup rootGroup, AvatarCollisionInfo avatarCollisionInfo,
			MeshSource meshSource)
	{
		super(gravity);
		this.meshSource = meshSource;

		this.instRecoToNif = instRecoToNif;
		dynamicsRootBranchGroup = new BranchGroup();
		dynamicsRootBranchGroup.setName("dynamicsRootBranchGroup");
		dynamicsRootBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		dynamicsRootBranchGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		dynamicsRootBranchGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		structureUpdateBehavior = new StructureUpdateBehavior();
		dynamicsRootBranchGroup.addChild(structureUpdateBehavior);

		rootGroup.addChild(dynamicsRootBranchGroup);

		this.avatarLocation = avatarCollisionInfo.getAvatarLocation();

		Transform3D rootTrans = new Transform3D(avatarLocation.getTransform());
		myNifBulletChar = new NBControlledChar(rootTrans, avatarCollisionInfo.getAvatarYHeight(), avatarCollisionInfo.getAvatarXZRadius());
		clientNifBulletCharBinding = new ClientNifBulletCharBinding(avatarLocation, myNifBulletChar);

		synchronized (instRecoBulletBindings)
		{
			instRecoBulletBindings.put(-999, clientNifBulletCharBinding);
		}
		synchronized (dynamicsWorld)
		{

			myNifBulletChar.addToDynamicsWorld(dynamicsWorld);
		}
	}

	public void setDisplayDebug(boolean displayDebug)
	{
		this.displayDebug = displayDebug;
	}

	boolean isDisplayDebug()
	{
		return this.displayDebug;
	}

	@Override
	protected void dynamicsPreStep()
	{
	}

	@Override
	protected void dynamicsPostStep()
	{
		if (displayDebug)
		{
			if (!destroyed)
			{
				if (!debugOutputInited)
				{
					DebugOutput.initDebug(dynamicsWorld, avatarLocation);
					debugOutputInited = true;
				}

				synchronized (dynamicsWorld)
				{
					LWJGL.step();
					//did it quit?
					if (!LWJGL.isDoRun())
						displayDebug = false;
				}
			}
		}
		else
		{
			if (debugOutputInited)
			{
				DebugOutput.disposeDebug();
				debugOutputInited = false;
			}
		}
	}

	public NBControlledChar getMyNifBulletChar()
	{
		return myNifBulletChar;
	}

	// we must clean up before being collected
	@Override
	public void finalize()
	{
		destroy();
	}

	@Override
	public void destroy()
	{
		this.destroyed = true;
		clear();
		dynamicsRootBranchGroup.detach();
	}

	public void clear()
	{
		instRecoBulletBindings.clear();
	}

	public BulletNifModel createRECO(J3dRECOInst j3dRECOInst)
	{

		if (recoIdToNifBullet.get(j3dRECOInst.getRecordId()) != null)
		{
			System.out.println("PhysicsDynamics, already loaded key " + j3dRECOInst.getRecordId() + " of " + j3dRECOInst);
			new Throwable("Thread:" + Thread.currentThread()).printStackTrace();
		}

		if (j3dRECOInst instanceof J3dLAND)
		{
			return createLand((J3dLAND) j3dRECOInst);
		}
		else
		{
			J3dRECOType j3dRECOType = j3dRECOInst.getJ3dRECOType();

			if (j3dRECOType != null && j3dRECOType.physNifFile != null)
			{
				// look for tes3 door with no controllers
				if (j3dRECOType instanceof J3dPivotDOOR)
				{
					// Look out! this is visual TG being used by physics!!!!
					return createStaticOrDynamic(j3dRECOInst, j3dRECOType.physNifFile, true);
				}
				else
				{
					return createStaticOrDynamic(j3dRECOInst, j3dRECOType.physNifFile, false);
				}
			}
			else if (j3dRECOInst instanceof J3dRECOChaInst)
			{
				createCharacter(j3dRECOInst);
			}
			else if (j3dRECOType != null && j3dRECOType.getName() != null && j3dRECOType.getName().equals("J3dGeneralSOUN"))
			{
				// sound is not part of phys
			}			
			else 
			{

				System.out.println(
						"j3dRECOType null or null phys " + j3dRECOType + " for inst " + j3dRECOInst + " " + j3dRECOInst.getRecordId());

			}
		}
		//System.out.println("add called total= " + nifBulletToRecoId.size());
		return null;
	}

	private NBSimpleModel createLand(J3dLAND j3dLAND)
	{
		Transform3D rootTrans = j3dLAND.getLocation(new Transform3D());
		NBSimpleModel nb = new NBSimpleModel(j3dLAND.getHeights(), rootTrans, J3dLAND.TERRIAN_SQUARE_SIZE);
		if (nb != null)
		{
			synchronized (recoIdToNifBullet)
			{
				recoIdToNifBullet.put(j3dLAND.getRecordId(), nb);
				nifBulletToRecoId.put(nb, j3dLAND.getRecordId());
			}
		}
		return nb;
	}

	private BulletNifModel createCharacter(J3dRECOInst j3dRECOInst)
	{
		BulletNifModel nb = null;

		//root should have scale in it
		Transform3D rootTrans = j3dRECOInst.getLocation(new Transform3D());

		// the nif file will have mass of 0 making this kinematic
		nb = new NBNonControlledChar(rootTrans, 0, j3dRECOInst.getRecordId());

		if (nb != null)
		{
			synchronized (recoIdToNifBullet)
			{
				recoIdToNifBullet.put(j3dRECOInst.getRecordId(), nb);
				nifBulletToRecoId.put(nb, j3dRECOInst.getRecordId());
			}
		}

		return nb;
	}

	/**
	 * the loading from file must occur not on the physics thread
	 * it will be happily done well before time, 
	 * then addRECO should happen on the physics tick  thread. 	
	 * 
	 * @param j3dRECOInst
	 * @param physNifFile
	 */
	private BulletNifModel createStaticOrDynamic(J3dRECOInst j3dRECOInst, String physNifFile, boolean hasPivot)
	{
		BulletNifModel nb = null;

		//root should have scale in it
		Transform3D rootTrans = j3dRECOInst.getLocation(new Transform3D());

		if (physNifFile != null && physNifFile.length() > 0)
		{
			NifFile nifFile = NifToJ3d.loadNiObjects(physNifFile, meshSource);
			if (BulletNifModelClassifier.isStaticModel(nifFile))
			{
				// the nif file will have mass of 0 making this static
				nb = new NBSimpleModel(physNifFile, meshSource, rootTrans, hasPivot);
			}
			else if (BulletNifModelClassifier.isKinematicModel(nifFile))
			{
				// the nif file will have mass of 0 making this kinematic
				nb = new NBSimpleModel(physNifFile, meshSource, rootTrans);
			}
			else if (BulletNifModelClassifier.isSimpleDynamicModel(nifFile, 0))
			{
				nb = createDynamic(j3dRECOInst, physNifFile);
			}
			else if (BulletNifModelClassifier.isComplexDynamic(nifFile))
			{
				//TODO: this bad boy right here
				//System.out.println("phys skipping isComplexDynamic " + physNifFile);
			}
			else
			{
				//TODO: lots of plants have this check them out 
				//System.out.println("crazy type? " + physNifFile);
				// probably just smoke effect etc, complex dynamic rag doll
				//System.out.println("phys skipping " + physNifFile);
			}

			if (nb != null)
			{
				synchronized (recoIdToNifBullet)
				{
					recoIdToNifBullet.put(j3dRECOInst.getRecordId(), nb);
					nifBulletToRecoId.put(nb, j3dRECOInst.getRecordId());
				}
			}

		}
		else
		{
			//Lights and alsorts of things can have no model or physics
			//System.out.println("why null phys? " + j3dRECOInst);
		}

		return nb;
	}

	private NBSimpleDynamicModel createDynamic(J3dRECOInst j3dRECOInst, String model)
	{
		NBSimpleDynamicModel nb = null;
		Transform3D rootTrans = j3dRECOInst.getLocation(new Transform3D());

		if (model != null && model.length() > 0)
		{

			//VELO velo = instReco.velocity;

			nb = new NBSimpleDynamicModel(model, meshSource, 0);

			//TODO: nif file should have mass, but for custom written might need to check and set set mass 
			//nb.getRootNifBulletbhkCollisionObject().getRigidBody().setMassProps(mass, inertia);

			Vector3f linearVelocity = new Vector3f();
			Vector3f rotationalVelocity = new Vector3f();
			//velo.getVelocities(linearVelocity, rotationalVelocity);
			nb.forceUpdate(rootTrans, linearVelocity, rotationalVelocity);

		}
		else
		{
			System.out.println("no model for createDynamic " + j3dRECOInst.getRecordId());
		}

		return nb;
	}

	public void updateRECOROTR(J3dRECOInst j3dRECOInst, Transform3D newTrans)
	{

		BulletNifModel nifBullet = recoIdToNifBullet.get(j3dRECOInst.getRecordId());
		if (nifBullet instanceof NBSimpleDynamicModel)
		{
			Quat4f q = new Quat4f();
			Vector3f v = new Vector3f();
			Utils3D.safeGetQuat(newTrans, q);
			newTrans.get(v);
			((NBSimpleDynamicModel) nifBullet).setTransform(q, v);
		}
		else if (nifBullet instanceof NBSimpleModel)
		{
			// TODO: this seems dodgy perhaps an exception here, surely it should be dynamic or kinematic?
			//remove re-create and re-add
			removeRECO(j3dRECOInst);
			createRECO(j3dRECOInst);
			addRECO(j3dRECOInst);
		}

	}

	public void updateRECOToggleOpen(J3dRECOInst j3dRECOInst, boolean isOpen)
	{
		BulletNifModel nifBullet = recoIdToNifBullet.get(j3dRECOInst.getRecordId());
		if (nifBullet instanceof NBSimpleModel)
		{
			NBSimpleModel nbKinematicModel = (NBSimpleModel) nifBullet;
			String seq = isOpen ? "Open" : "Close";// inst has already been updated (this is post)

			J3dNiControllerManager ncm = nbKinematicModel.getJ3dNiControllerManager();
			if (ncm != null)
			{
				J3dNiControllerSequence s = ncm.getSequence(seq);
				if (s != null)
				{
					s.fireSequenceOnce();
				}
			}
			else
			{
				if (nbKinematicModel.hasPivot())
				{
					nbKinematicModel.pivotTes3Door(isOpen);
				}
				else
				{
					//wow TES3 door have no animation, they look like they just artifically pivot around 
					System.out.println(
							"updateRECOToggleOpen door with no controller, probably travel door " + j3dRECOInst.getJ3dRECOType().getName());

					// drawers and chest in oblivion get the same issue
				}
			}
		}

	}

	/**
	 * @param j3dRECOInst
	 */
	protected void addRECO(J3dRECOInst j3dRECOInst)
	{
		//NOTE a create must have been called for this J3dRECOInst
		int recordId = j3dRECOInst.getRecordId();

		BulletNifModel nifBullet = recoIdToNifBullet.get(recordId);
		if (nifBullet != null)
		{
			try {
			// add to physics simulation
			synchronized (dynamicsWorld)
			{
				long startTimeInSynchBlock = System.currentTimeMillis();
				nifBullet.addToDynamicsWorld(dynamicsWorld);
				if (System.currentTimeMillis() - startTimeInSynchBlock > 50)
					System.err.println("TimeInSynchBlock bad " + (System.currentTimeMillis() - startTimeInSynchBlock));
			}

			if (nifBullet instanceof Node)
			{
				if (((Node) nifBullet).getParent() == null)
				{
					structureUpdateBehavior.add(dynamicsRootBranchGroup, (Node) nifBullet);
				}
				else
				{
					new Throwable("PhysicsDynamics attempt to re-add a node to scene! recordId " + recordId + " " + nifBullet)
							.printStackTrace();
				}
			}

			if (nifBullet instanceof NBSimpleDynamicModel)
			{
				NifBulletBinding irnbb = new InstRecoNifBulletBinding(j3dRECOInst, instRecoToNif, (NBSimpleDynamicModel) nifBullet);

				synchronized (instRecoBulletBindings)
				{
					instRecoBulletBindings.put(j3dRECOInst.getRecordId(), irnbb);
				}
			}
			}catch(AssertionError e) {
				//at com.bulletphysics.collision.dispatch.CollisionWorld.addCollisionObject(CollisionWorld.java:114)
				//assert !this.collisionObjects.contains(collisionObject);
				e.printStackTrace();
			}
		}
		else
		{
			//System.out.println("nifBullet == null in addRECO for inst "+j3dRECOInst.getRecordId());
		}

	}

	protected void removeRECO(J3dRECOInst j3dRECOInst)
	{
		int recordId = j3dRECOInst.getRecordId();
		BulletNifModel nifBullet = recoIdToNifBullet.get(recordId);
		if (nifBullet != null)
		{
			if (nifBullet instanceof Node)
				structureUpdateBehavior.remove(dynamicsRootBranchGroup, (Node) nifBullet);

			// remove from physics simulation
			synchronized (dynamicsWorld)
			{
				long startTimeInSynchBlock = System.currentTimeMillis();
				nifBullet.removeFromDynamicsWorld();
				nifBullet.destroy();
				if (System.currentTimeMillis() - startTimeInSynchBlock > 50)
					System.err.println("TimeInSynchBlock bad " + (System.currentTimeMillis() - startTimeInSynchBlock));
			}
			synchronized (instRecoBulletBindings)
			{
				instRecoBulletBindings.remove(recordId);
			}
			synchronized (recoIdToNifBullet)
			{
				nifBulletToRecoId.remove(nifBullet);
				recoIdToNifBullet.remove(recordId);
			}

		}
	}

	public void applyPhysicsToModel()
	{
		synchronized (instRecoBulletBindings)
		{
			for (int i = 0; i < instRecoBulletBindings.size(); i++)
			{
				NifBulletBinding instRecoNifBulletBinding = instRecoBulletBindings.get(instRecoBulletBindings.keyAt(i));
				if(instRecoNifBulletBinding != clientNifBulletCharBinding)
					instRecoNifBulletBinding.applyToModel();
			}
		}

	}
	
	public void applyControlledCharacterPhysicsToModel()
	{
		synchronized (instRecoBulletBindings)
		{
			if(clientNifBulletCharBinding != null) {
				clientNifBulletCharBinding.applyToModel();
			}
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
		return recoIdToNifBullet.get(recordId);
	}

	public int getRecordId(BulletNifModel nifBullet)
	{
		Integer id = null;
		synchronized (dynamicsWorld)
		{
			id = nifBulletToRecoId.get(nifBullet);
		}
		if (id == null)
		{
			return -1;
		}
		else
		{
			return id.intValue();
		}

	}

	public ClosestRayResultCallback findRayIntersect(Vector3f rayFrom, Vector3f rayTo, int characterRecordIdToIgnore)
	{
		try
		{
			CollisionWorld.ClosestRayResultCallback rayCallback = new ClosestRayResultCallbackChar(rayFrom, rayTo,
					characterRecordIdToIgnore);
			synchronized (dynamicsWorld)
			{
				dynamicsWorld.rayTest(rayFrom, rayTo, rayCallback);
			}
			return rayCallback;
		}
		catch (NullPointerException e)
		{
			System.out.println("findRayIntersect null again! something something ObjectPools");
			e.printStackTrace();
			return null;
		}
	}

	public static class ClosestRayResultCallbackChar extends CollisionWorld.ClosestRayResultCallback
	{
		public int characterRecordIdToIgnore;

		public ClosestRayResultCallbackChar(Vector3f rayFromWorld, Vector3f rayToWorld, int characterRecordIdToIgnore)
		{
			super(rayFromWorld, rayToWorld);
			this.characterRecordIdToIgnore = characterRecordIdToIgnore;
		}

		@Override
		public boolean needsCollision(BroadphaseProxy proxy0)
		{
			//FIXME: the id system should be much more general, for now only characters
			if (characterRecordIdToIgnore != -1)
			{
				if (proxy0.clientObject instanceof RigidBody)
				{
					RigidBody rb = (RigidBody) proxy0.clientObject;
					if (rb.getUserPointer() instanceof NBNonControlledChar)
					{
						NBNonControlledChar nbncc = (NBNonControlledChar) rb.getUserPointer();
						if (nbncc.getRecordId() == characterRecordIdToIgnore)
						{
							return false;
						}
					}
				}
			}

			return super.needsCollision(proxy0);
		}
	}

	public PhysicsStatus getPhysicsStatus()
	{
		PhysicsStatus ret = new PhysicsStatus();
		synchronized (recoIdToNifBullet)
		{
			for (int i = 0; i < recoIdToNifBullet.size(); i++)
			{
				BulletNifModel bnm = recoIdToNifBullet.get(recoIdToNifBullet.keyAt(i));

				if (bnm instanceof NBSimpleDynamicModel)
				{
					ret.dynCount++;
				}
				else if (bnm instanceof NBSimpleModel)
				{
					NBSimpleModel sm = (NBSimpleModel) bnm;
					ret.kinCount += sm.hasKinematics() ? 1 : 0;
					ret.staCount += !sm.hasKinematics() ? 1 : 0;
				}
			}
		}
		return ret;
	}

	public static class PhysicsStatus
	{
		public int dynCount = 0;

		public int kinCount = 0;

		public int staCount = 0;

		public long averageStepTimeMS = 0;
	}

}
