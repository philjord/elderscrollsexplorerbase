package scrollsexplorer.simpleclient.tes3;

import java.util.ArrayList;
import java.util.Enumeration;

import org.jogamp.java3d.Behavior;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.WakeupOnElapsedFrames;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

import esmj3d.j3d.J3dEffectNode;
import esmj3dtes3.ai.Tes3AICREA;
import esmj3dtes3.character.CharacterSheet;
import esmj3dtes3.data.records.NPCO;
import esmj3dtes3.j3d.character.CharacterAvatar;
import esmmanager.loader.IESMManager;
import nif.character.AttachedParts.Part;
import nif.character.NifCharacter;
import nif.j3d.J3dNiAVObject;
import nif.j3d.animation.J3dNiGeomMorpherController;
import scrollsexplorer.GameConfig;
import scrollsexplorer.simpleclient.SimpleBethCellManager;
import scrollsexplorer.simpleclient.SimpleWalkSetupInterface;
import tools3d.audio.SimpleSounds;
import tools3d.navigation.AvatarLocation;
import tools3d.utils.Utils3D;
import tools3d.utils.YawPitch;
import tools3d.utils.scenegraph.LocationUpdateListener;
import utils.source.MediaSources;

public class Tes3Extensions
{

	public static boolean ENABLE_HANDS = false;

	public static enum hands
	{
		NONE, AXE, SPELL
	};

	public static hands HANDS = hands.NONE;

	public static boolean dragon = false;

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
			SimpleWalkSetupInterface simpleWalkSetup2, SimpleBethCellManager simpleBethCellManager)
	{
		this.selectedGameConfig = selectedGameConfig;
		this.esmManager = esmManager;
		this.mediaSources = mediaSources;
		this.simpleWalkSetup = simpleWalkSetup2;
		this.simpleBethCellManager = simpleBethCellManager;

		FIRST_PERSON = !simpleWalkSetup.isTrailorCam();

		botBg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		botBg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		ArrayList<NPCO> npcos = new ArrayList<NPCO>();

		//npcos.add(new NPCO(1, "dragonscale_towershield"));
		//npcos.add(new NPCO(1, "imperial broadsword"));
		if (ENABLE_HANDS)
		{
			if (HANDS == hands.AXE)
			{
				npcos.add(new NPCO(1, "steel battle axe"));
			}

			if (HANDS == hands.NONE)
			{
				npcos.add(new NPCO(1, "common_shirt_04"));
				npcos.add(new NPCO(1, "common_shoes_04"));
				npcos.add(new NPCO(1, "common_pants_04"));
			}
			else if (HANDS == hands.AXE)
			{
				npcos.add(new NPCO(1, "dwemer_bracer_right"));
				npcos.add(new NPCO(1, "dwemer_bracer_left"));
				npcos.add(new NPCO(1, "dwemer_boots"));
				npcos.add(new NPCO(1, "dwemer_greaves"));
				//npcos.add(new NPCO(1, "dwemer_pauldron_right"));
				//npcos.add(new NPCO(1, "dwemer_pauldron_left"));
				npcos.add(new NPCO(1, "dwemer_cuirass"));
			}
			else if (HANDS == hands.SPELL)
			{
				npcos.add(new NPCO(1, "extravagant_shirt_01"));
				npcos.add(new NPCO(1, "common_shoes_04"));
				npcos.add(new NPCO(1, "extravagant_pants_01"));
			}
		}
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

			simpleWalkSetup.getVisualBranch().addChild(topBg);

		}

		returnToIdle();

		simpleWalkSetup.getWindow().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e)
			{
				for (int i = 0; i < e.getPointerCount(); i++)
				{
					int ex = e.getX(i);
					int ey = e.getY(i);
					//top half
					if (ey < (simpleWalkSetup.getWindow().getHeight() / 2))
					{
						// right side
						if (ex > (simpleWalkSetup.getWindow().getWidth() / 2))
						{
							mouseClickedUpperRight();
						}
						else
						{
							mouseClickedUpperLeft();
						}
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				for (int i = 0; i < e.getPointerCount(); i++)
				{
					int ex = e.getX(i);
					int ey = e.getY(i);
					//if it's third person and the lower half
					// this guy should play the walk forward (and turn animations?) in thrid person view!
					if (ey > (simpleWalkSetup.getWindow().getHeight() / 2))
					{
						if (!FIRST_PERSON)
						{
							if (ex < (simpleWalkSetup.getWindow().getWidth() / 2))
							{
								//hahah walk forward has motion in it!
								System.out.println("bottom press walkforward");
								avatarThirdPerson.playAnimation("turnleft", true);
							}
						}
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{

				for (int i = 0; i < e.getPointerCount(); i++)
				{
					int ex = e.getX(i);
					int ey = e.getY(i);
					// back to the idle bits, if it's third person and the lower half
					if (ey > (simpleWalkSetup.getWindow().getHeight() / 2))
					{
						if (!FIRST_PERSON)
						{
							if (ex < (simpleWalkSetup.getWindow().getWidth() / 2))
							{
								returnToIdle();
							}
						}
					}
				}
			}

		});

		if (dragon)
		{
			///umm??? Dragon?
			String skeletonNifFile = "actors\\dragon\\character assets\\skeleton.nif";
			ArrayList<String> skinNifFiles2 = new ArrayList<String>();
			skinNifFiles2.add("actors\\dragon\\character assets\\dragon.nif");

			String kff = "actors\\dragon\\animations\\mtidle_hover.kf";
			display(skeletonNifFile, skinNifFiles2, kff);
		}

	}

	private void display(String skeletonNifFile, ArrayList<String> skinNifFiles2, String kff)
	{

		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);

		ArrayList<String> idleAnimations = new ArrayList<String>();

		if (kff != null)
		{
			idleAnimations.add(kff);
		}

		// now add the root to the scene so the controller sequence is live

		NifCharacter nifCharacter = new NifCharacter(skeletonNifFile, skinNifFiles2, mediaSources, idleAnimations);
		nifCharacter.setCapability(Node.ALLOW_BOUNDS_READ);

		final TransformGroup tg = new TransformGroup();
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		Transform3D t = new Transform3D();
		t.rotY(Math.PI);
		t.setTranslation(new Vector3f(0, 0, -18.0f));
		tg.setTransform(t);
		tg.addChild(nifCharacter);
		bg.addChild(tg);
		simpleWalkSetup.getViewingPlatform().getPlatformGeometry().addChild(bg);

		Thread t2 = new Thread() {
			float y = 40;

			@Override
			public void run()
			{
				while (true)
				{
					try
					{
						Thread.sleep(30);
					}
					catch (InterruptedException e)
					{

						e.printStackTrace();
					}

					if (y > 2)
					{
						y = y - 0.1f;
						Transform3D t = new Transform3D();
						t.rotY(Math.PI);
						t.setTranslation(new Vector3f(0, y, -18.0f));
						tg.setTransform(t);
					}
				}
			}
		};
		t2.start();

	}

	private void returnToIdle()
	{
		if (FIRST_PERSON)
		{
			// idle, idle2, idle3...
			if (HANDS == hands.NONE)
				avatarFirstPerson.playAnimation("idle", true);
			else if (HANDS == hands.SPELL)
				avatarFirstPerson.playAnimation("idlespell", true);
			else if (HANDS == hands.AXE)
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

			// idle, idle2, idle3...
			if (HANDS == hands.NONE)
				avatarThirdPerson.playAnimation("idle", true);
			else if (HANDS == hands.SPELL)
				avatarThirdPerson.playAnimation("idlespell", true);
			else if (HANDS == hands.AXE)
				avatarThirdPerson.playAnimation("idle1h", true);
		}
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

			//Initialize
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

	private void mouseClickedUpperLeft()
	{
		if (ENABLE_HANDS)
		{
			if (!FIRST_PERSON)
			{
				if (!isspellhandsshowing)
				{
					showSpellHands();
				}
				else
				{
					castHealOnSelf();
				}
			}
			else
			{
				// do we need to get rid of axe and show cast hands?
				if (!isspellhandsshowing)
				{
					showSpellHands();
				}
				else
				{
					//time to casta  spell I say, cast target
					castSpellOnCREA();
				}
			}
		}

	}

	private boolean isaxeshow = true;
	private boolean isspellhandsshowing = false;

	private void mouseClickedUpperRight()
	{
		if (ENABLE_HANDS)
		{
			// do we need to get rid of axe and sow cast hands?
			if (!isaxeshow)
			{
				showAxeHands();
			}
			else
			{
				hitAxeCREA();
			}
		}

	}

	private void hitAxeCREA()
	{

		Thread t2 = new Thread() {
			@Override
			public void run()
			{
				try
				{
					if (!FIRST_PERSON)
					{
						avatarThirdPerson.playAnimation("weapononehand chop", false);
					}
					else
					{
						avatarFirstPerson.playAnimation("weapononehand chop", false);
					}
					Thread.sleep(800);
					// all CREA now react
					Tes3AICREA.beHitByAxe = true;

					Thread.sleep(400);
					returnToIdle();

					Tes3AICREA.beHitByAxe = false;

				}
				catch (InterruptedException e)
				{

					e.printStackTrace();
				}
			}
		};
		t2.start();
	}

	private void showAxeHands()
	{
		if (!FIRST_PERSON)
		{
			avatarThirdPerson.removePart(Part.Weapon);
			avatarThirdPerson.addPart(Part.Weapon, "w\\w_steel_battleaxe.nif");
			avatarThirdPerson.playAnimation("idle1h", true);
			SimpleSounds.playBackgroundSound(mediaSources.getSoundSource().getMediaContainer("Sound\\Fx\\item\\axeOUT.wav"), 10, 0);

		}
		else
		{
			avatarFirstPerson.removePart(Part.Weapon);
			avatarFirstPerson.addPart(Part.Weapon, "w\\w_steel_battleaxe.nif");
			avatarFirstPerson.playAnimation("idle1h", true);
			SimpleSounds.playBackgroundSound(mediaSources.getSoundSource().getMediaContainer("Sound\\Fx\\item\\axeOUT.wav"), 10, 0);

		}

		isspellhandsshowing = false;
		isaxeshow = true;
	}

	private void castSpellOnCREA()
	{
		Thread t2 = new Thread() {
			@Override
			public void run()
			{
				try
				{
					makeEffect("e\\magic_cast_ill.nif", "Sound\\Fx\\magic\\illuC.wav", "spellcast target");

					Thread.sleep(800);
					// all CREA now react
					Tes3AICREA.beHitBySpell = true;

					Thread.sleep(400);
					returnToIdle();

					// all CREA now react
					Tes3AICREA.beHitBySpell = false;

				}
				catch (InterruptedException e)
				{

					e.printStackTrace();
				}
			}
		};
		t2.start();
	}

	private void showSpellHands()
	{
		isspellhandsshowing = true;
		isaxeshow = false;

		if (!FIRST_PERSON)
		{
			avatarThirdPerson.removePart(Part.Weapon);
			avatarThirdPerson.playAnimation("idlespell", true);
			SimpleSounds.playBackgroundSound(mediaSources.getSoundSource().getMediaContainer("Sound\\Fx\\item\\axeAWAY.wav"), 10, 0);

		}
		else
		{
			avatarFirstPerson.removePart(Part.Weapon);
			avatarFirstPerson.playAnimation("idlespell", true);
			SimpleSounds.playBackgroundSound(mediaSources.getSoundSource().getMediaContainer("Sound\\Fx\\item\\axeAWAY.wav"), 10, 0);

		}
	}

	private void castHealOnSelf()
	{
		makeEffect("e\\magic_cast_restore.nif", "Sound\\Fx\\magic\\restC.wav", "spellcast self");
	}

	private void makeEffect(String nif, String wav, final String animation)
	{
		J3dEffectNode jen = new J3dEffectNode(nif, mediaSources);

		for (J3dNiAVObject j3dNiAVObject : jen.nvr.getNiToJ3dData().j3dNiAVObjectValues())
		{
			if (j3dNiAVObject.getJ3dNiTimeController() != null
					&& j3dNiAVObject.getJ3dNiTimeController() instanceof J3dNiGeomMorpherController)
			{
				((J3dNiGeomMorpherController) j3dNiAVObject.getJ3dNiTimeController()).fireFrameName("Frame_1", false);
			}
		}

		TransformGroup tg = new TransformGroup();
		Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(0, -0.9f, -0.0f));
		tg.setTransform(t);
		tg.addChild(jen);

		final BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.addChild(tg);

		BranchGroup soundBG = SimpleSounds.createPointSound(mediaSources.getSoundSource().getMediaContainer(wav), 10, 0);
		bg.addChild(soundBG);

		Thread t2 = new Thread() {
			@Override
			public void run()
			{
				try
				{
					if (avatarThirdPerson != null)
						avatarThirdPerson.playAnimation(animation, true);
					else
						avatarFirstPerson.playAnimation(animation, true);

					Thread.sleep(800);
					botBg.addChild(bg);
					Thread.sleep(1100);
					returnToIdle();
					Thread.sleep(1200);
					botBg.removeChild(bg);
				}
				catch (InterruptedException e)
				{

					e.printStackTrace();
				}
			}
		};
		t2.start();

	}

}
