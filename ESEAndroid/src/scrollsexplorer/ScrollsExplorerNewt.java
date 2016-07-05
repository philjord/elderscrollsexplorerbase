package scrollsexplorer;

import java.io.File;
import java.io.IOException;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

import archive.ArchiveFile;
import archive.BSArchiveSet;
import bsa.source.BsaMeshSource;
import bsa.source.BsaSoundSource;
import bsa.source.BsaTextureSource;
import esmj3d.j3d.BethRenderSettings;
import esmmanager.loader.ESMManager;
import esmmanager.loader.IESMManager;
import nativeLinker.LWJGLLinker;
import nif.BgsmSource;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import nif.character.NifCharacter;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.particles.tes3.J3dNiParticles;
import scrollsexplorer.simpleclient.BethWorldVisualBranch;
import scrollsexplorer.simpleclient.SimpleBethCellManager;
import scrollsexplorer.simpleclient.SimpleWalkSetup;
import scrollsexplorer.simpleclient.SimpleWalkSetupInterface;
import scrollsexplorer.simpleclient.physics.DynamicsEngine;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import scrollsexplorer.simpleclient.tes3.Tes3Extensions;
import tools.compressedtexture.CompressedTextureLoader;
import tools.io.ConfigLoader;
import tools3d.camera.Camera;
import tools3d.utils.ShaderSourceIO;
import tools3d.utils.YawPitch;
import tools3d.utils.loader.PropertyCodec;
import tools3d.utils.scenegraph.LocationUpdateListener;
import utils.source.EsmSoundKeyToName;
import utils.source.MediaSources;
import utils.source.MeshSource;
import utils.source.SoundSource;
import utils.source.TextureSource;

public class ScrollsExplorerNewt implements BethRenderSettings.UpdateListener, LocationUpdateListener
{
	//I think this auto installs itself 
	public DashboardNewt dashboardNewt = new DashboardNewt();

	private SimpleBethCellManager simpleBethCellManager;

	private SimpleWalkSetupInterface simpleWalkSetup;

	private MediaSources mediaSources;

	public IESMManager esmManager;

	public BSArchiveSet bsaFileSet;

	private GameConfig selectedGameConfig = null;

	private Tes3Extensions tes3Extensions;

	public ScrollsExplorerNewt(String gameToLoad)
	{
		//Setting to emulate Android requirements
		Camera.FRONT_CLIP = 0.2f;
		Camera.BACK_CLIP = 1000f;
		Camera.MIN_FRAME_CYCLE_TIME = 15;

		ESMManager.USE_FILE_MAPS = false;
		ESMManager.USE_MINI_CHANNEL_MAPS = true;
		ESMManager.USE_NON_NATIVE_ZIP = false;

		ArchiveFile.USE_FILE_MAPS = false;
		ArchiveFile.USE_MINI_CHANNEL_MAPS = true;
		ArchiveFile.USE_NON_NATIVE_ZIP = false;

		BethRenderSettings.setFarLoadGridCount(4);
		BethRenderSettings.setNearLoadGridCount(2);
		BethRenderSettings.setLOD_LOAD_DIST_MAX(32);
		BethRenderSettings.setObjectFade(100);
		BethRenderSettings.setItemFade(100);
		BethRenderSettings.setActorFade(50);
		BethWorldVisualBranch.LOAD_PHYS_FROM_VIS = false;
		DynamicsEngine.MAX_SUB_STEPS = 2;
		PhysicsSystem.MIN_TIME_BETWEEN_STEPS_MS = 40;
		NiGeometryAppearanceFactoryShader.setAsDefault();
		ShaderSourceIO.ES_SHADERS = false;// not the same!
		J3dNiTriBasedGeom.USE_FIXED_BOUNDS = true;
		// this definately doesn't help on desktop, but lots of methods calls so maybe?
		NifCharacter.BULK_BUFFER_UPDATES = false;

		NiGeometryAppearanceFactoryShader.setAsDefault();
		CompressedTextureLoader.setAnisotropicFilterDegree(4);

		//for big games go low spec
		if (!gameToLoad.equals("Morrowind"))
		{
			BethRenderSettings.setFarLoadGridCount(0);
			BethRenderSettings.setNearLoadGridCount(2);
			BethRenderSettings.setLOD_LOAD_DIST_MAX(0);
		}

		try
		{
			PropertyLoader.load();

			simpleWalkSetup = new SimpleWalkSetup("SimpleBethCellManager");
			simpleWalkSetup.setAzerty(false);

			simpleBethCellManager = new SimpleBethCellManager(simpleWalkSetup);

			BethRenderSettings.addUpdateListener(this);

			simpleWalkSetup.getAvatarLocation().addAvatarLocationListener(this);

			for (GameConfig gameConfig : GameConfig.allGameConfigs)
			{
				System.out.println("checking " + gameToLoad + " against " + gameConfig.gameName);
				if (gameConfig.gameName.equals(gameToLoad))
				{
					System.out.println("Found game to load! " + gameConfig.gameName);
					if (hasESMAndBSAFiles(gameConfig))
					{
						setSelectedGameConfig(gameConfig);
					}
					else
					{
						System.out.println("But it's not setup correctly!");
					}
					break;
				}
			}
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}

	}

	public void closingTime()
	{
		if (esmManager != null)
		{
			PropertyLoader.properties.setProperty("YawPitch" + esmManager.getName(),
					new YawPitch(simpleWalkSetup.getAvatarLocation().getTransform()).toString());
			PropertyLoader.properties.setProperty("Trans" + esmManager.getName(),
					"" + PropertyCodec.vector3fIn(simpleWalkSetup.getAvatarLocation().get(new Vector3f())));
			PropertyLoader.properties.setProperty("CellId" + esmManager.getName(), "" + simpleBethCellManager.getCurrentCellFormId());
		}
		PropertyLoader.save();

	}

	private static boolean hasESMAndBSAFiles(GameConfig gameConfig)
	{
		// check to ensure the esm file and at least one bsa file are in the folder
		File checkEsm = new File(gameConfig.scrollsFolder, gameConfig.mainESMFile);
		if (!checkEsm.exists())
		{
			return false;
		}

		int countOfBsa = 0;
		File checkBsa = new File(gameConfig.scrollsFolder);
		for (File f : checkBsa.listFiles())
		{
			countOfBsa += f.getName().toLowerCase().endsWith(".bsa") ? 1 : 0;
			countOfBsa += f.getName().toLowerCase().endsWith(".ba2") ? 1 : 0;
		}

		if (countOfBsa == 0)
		{
			return false;
		}

		return true;
	}

	@Override
	public void renderSettingsUpdated()
	{
		simpleBethCellManager.updateBranches();
	}

	/**
	 
	 */
	private void setSelectedGameConfig(GameConfig newGameConfig)
	{
		selectedGameConfig = newGameConfig;
		simpleWalkSetup.getAvatarCollisionInfo().setAvatarYHeight(selectedGameConfig.avatarYHeight);

		Thread t = new Thread() {
			public void run()
			{
				synchronized (selectedGameConfig)
				{
					IDashboard.dashboard.setEsmLoading(1);

					esmManager = ESMManager.getESMManager(selectedGameConfig.getESMPath());
					bsaFileSet = null;
					if (esmManager != null)
					{
						YawPitch yp = YawPitch
								.parse(PropertyLoader.properties.getProperty("YawPitch" + esmManager.getName(), new YawPitch().toString()));
						Vector3f trans = PropertyCodec.vector3fOut(PropertyLoader.properties.getProperty("Trans" + esmManager.getName(),
								selectedGameConfig.startLocation.toString()));
						int prevCellformid = Integer.parseInt(PropertyLoader.properties.getProperty("CellId" + esmManager.getName(), "-1"));
						simpleWalkSetup.getAvatarLocation().set(yp.get(new Quat4f()), trans);

						if (prevCellformid == -1)
						{
							prevCellformid = selectedGameConfig.startCellId;
						}

						new EsmSoundKeyToName(esmManager);
						MeshSource meshSource;
						TextureSource textureSource;
						SoundSource soundSource;

						if (bsaFileSet == null)
						{
							bsaFileSet = new BSArchiveSet(new String[] { selectedGameConfig.scrollsFolder }, true);
						}

						if (bsaFileSet.size() == 0)
						{
							System.err.println("bsa files size is 0 :(");
							IDashboard.dashboard.setEsmLoading(-1);
							return;
						}

						meshSource = new BsaMeshSource(bsaFileSet);
						textureSource = new BsaTextureSource(bsaFileSet);
						soundSource = new BsaSoundSource(bsaFileSet, new EsmSoundKeyToName(esmManager));

						//Just for the crazy new fallout 4 system
						BgsmSource.setBgsmSource(meshSource);

						mediaSources = new MediaSources(meshSource, textureSource, soundSource);

						simpleWalkSetup.configure(meshSource, simpleBethCellManager);
						simpleWalkSetup.setEnabled(false);
						//TODO: can't do this now with teh hold context set to true
						//simpleWalkSetup.getWindow().setFullscreen(true);

						//FIXME: stops working once fully running, but responds up to that point
						// that is to say the button no longer sends anything through
						// button won't work off the event thread, so I need to add my own system in and ignore the button
						// button only runs if display is called on the window but that cuts FPS in half
						simpleWalkSetup.getWindow().setDefaultCloseOperation(WindowClosingMode.DISPOSE_ON_CLOSE);
						simpleWalkSetup.getWindow().addWindowListener(new WindowAdapter() {
							@Override
							public void windowDestroyed(WindowEvent e)
							{
								System.out.println("X buttons works now");
								closingTime();
								System.exit(0);
							}

							@Override
							public void windowResized(final WindowEvent e)
							{
								J3dNiParticles.screenWidth = simpleWalkSetup.getWindow().getWidth();
							}

						});
						J3dNiParticles.screenWidth = simpleWalkSetup.getWindow().getWidth();
						simpleWalkSetup.getWindow().addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(KeyEvent e)
							{
								if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
								{
									simpleWalkSetup.closingTime();
									closingTime();
									System.exit(0);
								}
							}
						});

						// I could use the j3dcellfactory now? with the cached cell records?
						simpleBethCellManager.setSources(selectedGameConfig, esmManager, mediaSources);

						if (selectedGameConfig == GameConfig.allGameConfigs.get(0))
						{
							System.out.println("Adding Tes3 extensions");
							tes3Extensions = new Tes3Extensions(selectedGameConfig, esmManager, mediaSources, simpleWalkSetup,
									simpleBethCellManager);
						}

						display(prevCellformid);

					}
					else
					{
						System.out.println("esm manger is null, I just don't know why...");
					}

					IDashboard.dashboard.setEsmLoading(-1);
				}

			}
		};
		t.setName("setSelectedGameConfig " + newGameConfig.gameName);
		t.start();
	}

	@Override
	public void locationUpdated(Quat4f rot, Vector3f trans)
	{

	}

	private void display(final int cellformid)
	{
		Vector3f t = simpleWalkSetup.getAvatarLocation().get(new Vector3f());
		Quat4f r = simpleWalkSetup.getAvatarLocation().get(new Quat4f());
		simpleBethCellManager.setCurrentCellFormId(cellformid, t, r);
	}

	public SimpleBethCellManager getSimpleBethCellManager()
	{
		return simpleBethCellManager;
	}

	public SimpleWalkSetupInterface getSimpleWalkSetup()
	{
		return simpleWalkSetup;
	}

	private static void setDebug(boolean b)
	{
		if (b)
		{
			System.out.println("DEBUG ON");
			// leave settings alone for optional debug parts
		}
		else
		{

		}
	}

	public static void main(String[] args)
	{
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("j3d.cacheAutoComputeBounds", "true");

		ConfigLoader.loadConfig(args);

		// always load lwjgl for jbullet debug
		//Note this mian won't be called on Android so hopefully the linker never causes trouble
		new LWJGLLinker();

		if (args.length > 0 && args[0].equals("debug"))
		{
			ScrollsExplorerNewt.setDebug(true);
		}
		else
		{
			ScrollsExplorerNewt.setDebug(false);
		}

		new ScrollsExplorerNewt(args[0]);
	}

}
