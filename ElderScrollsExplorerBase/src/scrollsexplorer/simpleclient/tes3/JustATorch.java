package scrollsexplorer.simpleclient.tes3;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Vector3f;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;

import esfilemanager.loader.IESMManager;
import esmj3d.j3d.BethRenderSettings;
import scrollsexplorer.GameConfig;
import scrollsexplorer.simpleclient.SimpleBethCellManager;
import scrollsexplorer.simpleclient.SimpleWalkSetupInterface;
import utils.source.MediaSources;

public class JustATorch {

	protected static boolean			FIRST_PERSON	= false;
	protected GameConfig				selectedGameConfig;
	protected IESMManager				esmManager;
	protected MediaSources				mediaSources;
	protected SimpleWalkSetupInterface	simpleWalkSetup;
	protected SimpleBethCellManager		simpleBethCellManager;

	protected BranchGroup				botBg			= new BranchGroup();

	protected Torch						characterTorch	= null;

	public JustATorch(	GameConfig selectedGameConfig, IESMManager esmManager, final MediaSources mediaSources,
						SimpleWalkSetupInterface simpleWalkSetup2, SimpleBethCellManager simpleBethCellManager) {
		this.selectedGameConfig = selectedGameConfig;
		this.esmManager = esmManager;
		this.mediaSources = mediaSources;
		this.simpleWalkSetup = simpleWalkSetup2;
		this.simpleBethCellManager = simpleBethCellManager;

		FIRST_PERSON = !simpleWalkSetup.isTrailorCam();

		botBg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		botBg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		characterTorch = new Torch(mediaSources);
		// spot light for a flash light
		//characterTorch = new Torch(mediaSources, true, new Color3f(0.9f, 0.86f, 0.85f), new Point3f(0, -0.2f, 0), new Point3f(0, 1.1f, 0),
		//		new Vector3f(0,0,1), 20f, 0f, -1f);

		if (FIRST_PERSON) {

			TransformGroup tg = new TransformGroup();
			Transform3D t = new Transform3D();
			t.setTranslation(new Vector3f(0, -0.25f, -0.0f));
			tg.setTransform(t);

			tg.addChild(characterTorch);

			botBg.addChild(tg);

		} else {
			// trailor cam view

			TransformGroup tg = new TransformGroup();
			Transform3D t = new Transform3D();
			t.setTranslation(new Vector3f(0, -0.25f, 0.0f));
			tg.setTransform(t);
			tg.addChild(characterTorch);
			botBg.addChild(tg);
		}

		simpleWalkSetup.getViewingPlatform().getPlatformGeometry().addChild(botBg);
		simpleWalkSetup.getWindow().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_T) {
					BethRenderSettings.setEnableTorchLight(!BethRenderSettings.isEnableTorchLight());
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		});

	}

	public void setTorchLightEnabled(boolean enabled) {
		characterTorch.setEnable(enabled);
	}

}
