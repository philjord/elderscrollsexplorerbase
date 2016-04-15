package scrollsexplorer.simpleclient.scenegraph;

import java.util.Random;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import nif.j3d.animation.J3dNiControllerManager;
import scrollsexplorer.GameConfig;
import tools3d.utils.scenegraph.SpinTransform;
import utils.source.MediaSources;

public class LoadScreen extends BranchGroup
{
	private GameConfig gameConfig;

	private MediaSources mediaSources;

	private BranchGroup currentLoadScreenBG;

	private TransformGroup currentLoadScreenTG;

	private TransformGroup spinTransformGroup = new TransformGroup();

	public LoadScreen(GameConfig gameConfig, MediaSources mediaSources)
	{
		this.gameConfig = gameConfig;
		this.mediaSources = mediaSources;
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

	}

	public void setShowLoadScreen(boolean show)
	{
		if (!show)
		{
			if (currentLoadScreenBG != null)
			{
				currentLoadScreenBG.detach();
				currentLoadScreenBG = null;
			}
		}
		else
		{
			currentLoadScreenBG = new BranchGroup();
			currentLoadScreenBG.setCapability(BranchGroup.ALLOW_DETACH);

			currentLoadScreenTG = new TransformGroup();

			currentLoadScreenBG.addChild(currentLoadScreenTG);

			String nifFile = getRandom(gameConfig.loadScreens);

			if (mediaSources.getMeshSource().nifFileExists(nifFile))
			{
				NifJ3dVisRoot nif = NifToJ3d.loadShapes(nifFile, mediaSources.getMeshSource(), mediaSources.getTextureSource());
				if (nif != null)
				{
					J3dNiAVObject j3dNiAVObject = nif.getVisualRoot();

					if (j3dNiAVObject != null)
					{
						BoundingSphere bounds = (BoundingSphere) j3dNiAVObject.getBounds();

						// adjust the center to the center of the spin
						TransformGroup centreriser = new TransformGroup();
						Transform3D tc = new Transform3D();
						Point3d p = new Point3d();
						bounds.getCenter(p);
						Vector3f vtc = new Vector3f(p);
						vtc.negate();
						tc.setTranslation(vtc);
						centreriser.setTransform(tc);

						for (J3dNiAVObject av : nif.getNiToJ3dData().j3dNiAVObjectValues())
						{
							if (av.getJ3dNiControllerManager() != null)
							{
								//note self cleaning uping
								ControllerInvokerThread controllerInvokerThread = new ControllerInvokerThread(nif.getVisualRoot().getName(),
										av.getJ3dNiControllerManager(), null);
								controllerInvokerThread.start();
							}
							if (av.getJ3dNiTimeController() != null)
							{
								System.out.println("I wish I could fire this :( " + av.getJ3dNiTimeController());
								System.out.println("But it's a skin instance so I need like a character!");
								System.out.println("Or are they in fact being fired, but the skins not listening t the bones");
								System.out.println("automaticallly? perhaps it should, even without a character to cock about");
							}
						}

						spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
						spinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
						spinTransformGroup.addChild(centreriser);
						centreriser.addChild(j3dNiAVObject);

						currentLoadScreenTG.addChild(spinTransformGroup);

						SpinTransform spinTransform = new SpinTransform(spinTransformGroup, 0.20);
						spinTransform.setEnable(true);
						currentLoadScreenTG.addChild(spinTransform);

						// forward  a bit and down a bit
						float radius = (float) bounds.getRadius();
						Vector3f v = new Vector3f(0, -radius / 2f, -radius * 2f);
						Quat4f q = new Quat4f(0, 0, 0, 1);
						Transform3D t = new Transform3D();
						t.set(q, v, 1f);

						currentLoadScreenTG.setTransform(t);

					}

				}
			}
			//TODO: note not on structure behavior, trouble?
			addChild(currentLoadScreenBG);
		}

	}

	public static String getRandom(String[] array)
	{
		int rnd = new Random().nextInt(array.length);
		return array[rnd];
	}

	public class ControllerInvokerThread extends Thread
	{
		private J3dNiControllerManager cont;

		private J3dNiControllerManager optionalCont;

		public ControllerInvokerThread(String name, J3dNiControllerManager cont, J3dNiControllerManager optionalCont)
		{

			this.setDaemon(true);
			this.setName("ControllerInvokerThread " + name);

			this.cont = cont;
			this.optionalCont = optionalCont;
		}

		@Override
		public void run()
		{
			try
			{
				Thread.sleep(1000);

				String[] actions = cont.getAllSequences();
				while (cont.isLive())
				{
					for (int i = 0; i < actions.length; i++)
					{
						Thread.sleep((long) (Math.random() * 3000) + 1000);
						System.out.println("firing " + actions[i]);

						cont.getSequence(actions[i]).fireSequenceOnce();

						if (optionalCont != null)
							optionalCont.getSequence(actions[i]).fireSequenceOnce();

						Thread.sleep(cont.getSequence(actions[i]).getLengthMS());
					}
				}
			}
			catch (InterruptedException e)
			{
			}
		}

	}

}
