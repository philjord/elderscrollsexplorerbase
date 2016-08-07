package scrollsexplorer.simpleclient.tes3;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PointSound;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

import esmj3d.j3d.J3dEffectNode;
import esmj3dtes3.character.CharacterSheet;
import esmj3dtes3.data.records.NPCO;
import esmj3dtes3.j3d.character.CharacterAvatar;
import esmmanager.loader.IESMManager;
import nif.j3d.J3dNiAVObject;
import nif.j3d.animation.J3dNiGeomMorpherController;
import scrollsexplorer.GameConfig;
import scrollsexplorer.simpleclient.SimpleBethCellManager;
import scrollsexplorer.simpleclient.SimpleWalkSetup;
import scrollsexplorer.simpleclient.SimpleWalkSetupInterface;
import tools3d.audio.SimpleSounds;
import tools3d.navigation.AvatarLocation;
import tools3d.utils.Utils3D;
import tools3d.utils.YawPitch;
import tools3d.utils.scenegraph.LocationUpdateListener;
import utils.source.MediaSources;

public class Tes3Extensions
{

	private static boolean FIRST_PERSON = false;;
	private GameConfig selectedGameConfig;
	private IESMManager esmManager;
	private MediaSources mediaSources;
	private SimpleWalkSetupInterface simpleWalkSetup;
	private SimpleBethCellManager simpleBethCellManager;

	private CharacterSheet characterSheet;
	private CharacterAvatar avatarFirstPerson;
	private CharacterAvatar avatarThirdPerson;
	private BranchGroup botBg = new BranchGroup();

	public Tes3Extensions(GameConfig selectedGameConfig, IESMManager esmManager, final MediaSources mediaSources,
			SimpleWalkSetupInterface simpleWalkSetup, SimpleBethCellManager simpleBethCellManager)
	{
		this.selectedGameConfig = selectedGameConfig;
		this.esmManager = esmManager;
		this.mediaSources = mediaSources;
		this.simpleWalkSetup = simpleWalkSetup;
		this.simpleBethCellManager = simpleBethCellManager;

		FIRST_PERSON = !simpleWalkSetup.isTrailorCam();

		botBg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		botBg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		ArrayList<NPCO> npcos = new ArrayList<NPCO>();

		npcos.add(new NPCO(1, "bonemold_bracer_left"));
		npcos.add(new NPCO(1, "bonemold_bracer_right"));
		//npcos.add(new NPCO(1, "dragonscale_towershield"));
		//npcos.add(new NPCO(1, "imperial broadsword"));		
		npcos.add(new NPCO(1, "steel battle axe"));

		npcos.add(new NPCO(1, "common_shirt_04"));
		npcos.add(new NPCO(1, "common_shoes_04"));
		npcos.add(new NPCO(1, "common_pants_04"));

		characterSheet = new CharacterSheet(false, "Breton", "b_n_breton_f_head_01", "b_n_breton_f_hair_01", npcos);

		if (FIRST_PERSON)
		{
			avatarFirstPerson = new CharacterAvatar(characterSheet, esmManager, mediaSources, true);

			TransformGroup tg = new TransformGroup();
			Transform3D t = new Transform3D();
			//t.rotY(Math.PI / 2f);
			t.setTranslation(new Vector3f(0, -1.50f, -0.0f));
			//t.rotY(Math.PI / -2f);
			//t.setTranslation(new Vector3f(0, 0, -3f));
			tg.setTransform(t);
			tg.addChild(avatarFirstPerson);

			botBg.addChild(tg);

			simpleWalkSetup.getViewingPlatform().getPlatformGeometry().addChild(botBg);

			//avatarFirstPerson.playAnimation("idlespell", true);
			avatarFirstPerson.playAnimation("idle1h", true);

			//TODO: how do 2 handed weapons work what is idle2c and idle2w? close and wide?
			// yep WEAP type has the types

			//idle1h
			//idle2c
			//idle2w
			//idlehh
			//idlecrossbow
			//idlespell
			//idlestorm
			//idle

		}
		else
		{
			// trailor cam view

			// need a transformGroup and BranchGroup pair to hold the avatar
			TransformGroup cameraTG = new TransformGroup();
			cameraTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

			BranchGroup topBg = new BranchGroup();
			topBg.addChild(cameraTG);

			AvatarFollower af = new AvatarFollower(cameraTG, simpleWalkSetup.getAvatarLocation());
			topBg.addChild(af);
			af.setSchedulingBounds(Utils3D.defaultBounds);
			af.setEnable(true);

			cameraTG.addChild(botBg);

			avatarThirdPerson = new CharacterAvatar(characterSheet, esmManager, mediaSources, false);

			TransformGroup tg = new TransformGroup();
			Transform3D t = new Transform3D();
			t.setTranslation(new Vector3f(0, -0.9f, 0.0f));
			tg.setTransform(t);
			tg.addChild(avatarThirdPerson);

			botBg.addChild(tg);
			avatarThirdPerson.playAnimation("idle1h", true);

			simpleWalkSetup.getVisualBranch().addChild(topBg);

		}

		simpleWalkSetup.getWindow().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e)
			{
				//avatarFirstPerson.playAnimation("idle1h", true);

				// TODO: need WeaponOneHAnd: Chop Start to fire now
				// text key 136 in base_anim

				// then need to ensure it returns to idle

				// but I also want the walkforward animations to play when I'm walking forawrds?
				// so I need to be a listener to the player navigation code

				//WalkForward1h: Start index 29

				// SpellCast: Touch Start index 557 notice there is no sound gen in the keys

				//SPEL doesn't seem to have it either
				// maybe SPEL refers to ENCH ? or MGEF ? mgef looks like the one, presumably spel refers to mgef?

			}

		});

		Thread t = new Thread() {
			public void run()
			{
				try
				{
					Thread.sleep(5000);

					BranchGroup soundBG = SimpleSounds
							.createPointSound(mediaSources.getSoundSource().getMediaContainer("Sound\\Fx\\envrn\\watr_wave.wav"));
					botBg.addChild(soundBG);
					soundBG = SimpleSounds.createPointSound(mediaSources.getSoundSource().getMediaContainer("Sound\\Cr\\silt\\silt01.wav"));
					botBg.addChild(soundBG);

				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		};
		t.start();

		Thread t2 = new Thread() {
			public void run()
			{
				try
				{
					Thread.sleep(10000);

					J3dEffectNode jen = new J3dEffectNode("e\\magic_cast_restore.nif", mediaSources);

					for (J3dNiAVObject j3dNiAVObject : jen.nvr.getNiToJ3dData().j3dNiAVObjectValues())
					{
						if (j3dNiAVObject.getJ3dNiTimeController() != null
								&& j3dNiAVObject.getJ3dNiTimeController() instanceof J3dNiGeomMorpherController)
						{
							((J3dNiGeomMorpherController) j3dNiAVObject.getJ3dNiTimeController()).fireFrameName("Frame_1");
						}
					}

					TransformGroup tg = new TransformGroup();
					Transform3D t = new Transform3D();
					t.setTranslation(new Vector3f(0, -0.9f, -0.0f));
					tg.setTransform(t);
					tg.addChild(jen);

					BranchGroup bg = new BranchGroup();
					bg.setCapability(BranchGroup.ALLOW_DETACH);
					bg.addChild(tg);
					botBg.addChild(bg);
					Thread.sleep(2000);

					bg.detach();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		};
		t2.start();

	}

	private class AvatarFollower extends Behavior implements LocationUpdateListener
	{
		private WakeupOnElapsedFrames FPSWakeUp = new WakeupOnElapsedFrames(0);

		private TransformGroup cameraTG;

		private Transform3D t = new Transform3D();
		private YawPitch yp = new YawPitch();
		private Quat4f ypRot = new Quat4f();

		public AvatarFollower(TransformGroup cameraTG, AvatarLocation avatarLocation)
		{
			this.cameraTG = cameraTG;
			avatarLocation.addAvatarLocationListener(this);

			//initialise
			Quat4f rot = new Quat4f();
			Vector3f trans = new Vector3f();
			avatarLocation.get(rot, trans);
			locationUpdated(rot, trans);
		}

		@Override
		public void initialize()
		{
			wakeupOn(FPSWakeUp);
		}

		@Override
		public void processStimulus(Enumeration critera)
		{
			cameraTG.setTransform(t);
			// Set the trigger for the behavior
			wakeupOn(FPSWakeUp);
		}

		@Override
		public void locationUpdated(Quat4f rot, Vector3f trans)
		{
			yp.set(rot);
			yp.setPitch(0);
			yp.get(ypRot);

			t.set(ypRot, trans, 1);
		}
	}

}
