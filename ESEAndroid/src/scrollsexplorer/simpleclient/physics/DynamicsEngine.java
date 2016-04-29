package scrollsexplorer.simpleclient.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.broadphase.HashedOverlappingPairCache;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.dispatch.DefaultNearCallback;
import com.bulletphysics.collision.dispatch.GhostPairCallback;
import com.bulletphysics.dynamics.ActionInterface;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.extras.gimpact.GImpactCollisionAlgorithm;
import com.bulletphysics.linearmath.Clock;

public abstract class DynamicsEngine
{
	public static int MAX_SUB_STEPS = 2;

	protected Clock timeKeeper = new Clock();

	private BroadphaseInterface broadphase;

	private CollisionDispatcher dispatcher;

	private ConstraintSolver solver;

	private CollisionConfiguration collisionConfiguration;

	protected DynamicsWorld dynamicsWorld;

	private boolean paused = true;

	private boolean skipStepSim = false;

	private long[] recentStepTimes = new long[10];

	private int recentStepTimesHead = 0;

	/**
	 * Note this requries the dynamicsUpdate to be called on a regular basis by whatever teh mian thread is
	 * Try to call it at least 60 per second (16ms) or more often if possible
	 * @param interior
	 * @param gravity
	 * @param physicsScaling
	 */
	public DynamicsEngine(Vector3f gravity)
	{

		// collision configuration contains default setup for memory, collision setup
		collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
		dispatcher = new CollisionDispatcher(collisionConfiguration);
		dispatcher.setNearCallback(new DefaultNearCallback());

		broadphase = new DbvtBroadphase(new HashedOverlappingPairCache());

		//set up the ghost pair call back thing
		broadphase.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());

		// the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
		solver = new SequentialImpulseConstraintSolver();

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

		// register algorithm  for giimpact
		GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);

		dynamicsWorld.setGravity(gravity);

		timeKeeper.reset();

	}

	public boolean isSkipStepSim()
	{
		return skipStepSim;
	}

	public void setSkipStepSim(boolean skipStepSim)
	{
		this.skipStepSim = skipStepSim;
	}

	private long timeOfLastStep = System.currentTimeMillis() * 2;

	public void dynamicsTick()
	{
		long dtms = timeKeeper.getTimeMicroseconds();
		timeKeeper.reset();

		long startOfPreStep = System.currentTimeMillis();
		dynamicsPreStep();
		if (System.currentTimeMillis() - startOfPreStep > 100)
			System.err.println("startOfPreStep update long " + (System.currentTimeMillis() - startOfPreStep));

		// step the simulation
		if (dynamicsWorld != null)
		{
			try
			{
				long dtms2 = timeKeeper.getTimeMicroseconds();

				long startOfSynch = System.currentTimeMillis();
				synchronized (dynamicsWorld)
				{
					if ((System.currentTimeMillis() - startOfSynch) > 20)
						System.err.println("synchronized (dynamicsWorld) long " + (System.currentTimeMillis() - startOfSynch));

					if (!skipStepSim)
					{
						if (System.currentTimeMillis() - timeOfLastStep > 200)
							System.err.println("Physics step stuttered " + (System.currentTimeMillis() - timeOfLastStep));
						timeOfLastStep = System.currentTimeMillis();

						//note timeStep is seconds not ms AND you must have a sub step count! make him 5ish
						long startOfStepSimulation = System.currentTimeMillis();
						dynamicsWorld.stepSimulation(dtms / 1000000f, MAX_SUB_STEPS);
						if (System.currentTimeMillis() - startOfStepSimulation > 200)
							System.err.println("stepSimulation long " + (System.currentTimeMillis() - startOfStepSimulation));
					}
					else
					{
						long startOfActions = System.currentTimeMillis();
						//fire actions any way?
						for (int a = 0; a < dynamicsWorld.getNumActions(); a++)
						{
							ActionInterface ai = dynamicsWorld.getAction(a);
							ai.updateAction(dynamicsWorld, dtms / 1000000f);
						}
						if (System.currentTimeMillis() - startOfActions > 100)
							System.err.println("Action updates long " + (System.currentTimeMillis() - startOfActions));

					}
				}

				// chuck it in the recent step time
				addStepTime(((timeKeeper.getTimeMicroseconds() - dtms2) / 1000));
			}
			catch (NullPointerException e)
			{
				//DbvtBroadphase.setAabb being crazy
				if (e.getStackTrace().length > 0)
					System.out.println("DbvtBroadphase.setAabb being crazy" + e + " " + e.getStackTrace()[0]);
				
				e.printStackTrace();
			}
			catch (ClassCastException e)
			{
				// used to be called when ObjectPool was not properly multi threaded, should be fixed now
				if (e.getStackTrace().length > 0)
					System.out.println("used to be called when ObjectPool was not properly multi threaded, should be fixed now" + e + " "
							+ e.getStackTrace()[0]);
			}

		}
		long startOfPostStep = System.currentTimeMillis();
		dynamicsPostStep();
		if (System.currentTimeMillis() - startOfPostStep > 100)
			System.err.println("dynamicsPostStep update long " + (System.currentTimeMillis() - startOfPostStep));

	}

	protected abstract void dynamicsPostStep();

	protected abstract void dynamicsPreStep();

	@Override
	public void finalize()
	{
		destroy();
	}

	public synchronized void destroy()
	{
	}

	public void pause()
	{
		paused = true;
	}

	public void unpause()
	{
		paused = false;
	}

	public boolean isPaused()
	{
		return paused;
	}

	private void addStepTime(long time)
	{
		recentStepTimes[recentStepTimesHead] = time;
		recentStepTimesHead++;
		recentStepTimesHead = recentStepTimesHead >= recentStepTimes.length ? 0 : recentStepTimesHead;

	}

	public int getAverageStepTimeMS()
	{
		int average = 0;

		for (long time : recentStepTimes)
			average += time;

		return average / recentStepTimes.length;
	}

	public int getNumCollisionObjects()
	{
		return dynamicsWorld.getNumCollisionObjects();
	}
}
