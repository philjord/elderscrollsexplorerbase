package scrollsexplorer.simpleclient;

import javax.vecmath.Color3f;
import javax.vecmath.Point2f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.jogamp.java3d.AmbientLight;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.DirectionalLight;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Light;
import org.jogamp.java3d.ShaderError;
import org.jogamp.java3d.ShaderErrorListener;

import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestRayResultCallback;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.sun.j3d.utils.universe.ViewingPlatform;

import esmj3d.j3d.BethRenderSettings;
import nifbullet.NavigationProcessorBullet;
import nifbullet.NavigationProcessorBullet.NbccProvider;
import nifbullet.cha.NBControlledChar;
import scrollsexplorer.IDashboard;
import scrollsexplorer.simpleclient.mouseover.ActionableMouseOverHandler;
import scrollsexplorer.simpleclient.mouseover.AdminMouseOverHandler;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import scrollsexplorer.simpleclient.scenegraph.LoadingInfoBehavior;
import tools3d.camera.CameraPanel;
import tools3d.camera.HMDCamDolly;
import tools3d.camera.HMDCameraPanel;
import tools3d.camera.HeadCamDolly;
import tools3d.camera.ICameraPanel;
import tools3d.camera.TrailerCamDolly;
import tools3d.camera.TrailerCamDolly.TrailorCamCollider;
import tools3d.mixed3d2d.Canvas3D2D;
import tools3d.mixed3d2d.curvehud.elements.HUDCompass;
import tools3d.mixed3d2d.curvehud.elements.HUDCrossHair;
import tools3d.mixed3d2d.curvehud.elements.HUDFPSCounter;
import tools3d.mixed3d2d.curvehud.elements.HUDPosition;
import tools3d.mixed3d2d.curvehud.elements.HUDText;
import tools3d.navigation.AvatarCollisionInfo;
import tools3d.navigation.AvatarLocation;
import tools3d.navigation.NavigationInputNewtKey;
import tools3d.navigation.NavigationInputNewtMouseLocked;
import tools3d.navigation.NavigationTemporalBehaviour;
import tools3d.ovr.OculusException;
import tools3d.universe.VisualPhysicalUniverse;
import utils.source.MeshSource;

/**
 * A class to pull the keyboard nav, bullet phys, nif displayable, canvas2d3d overlays, 
 * physics display together, 
 * 
 * but no particular way to load nifs, esm, comms or anything else
 * 
 * @author philip
 *
 */
public class SimpleWalkSetup implements SimpleWalkSetupInterface
{
	public static boolean HMD_MODE = false;
	public static boolean TRAILER_CAM = false;

	//	private JFrame frame = new JFrame();

	private boolean enabled = false;

	public VisualPhysicalUniverse universe;

	private BranchGroup modelGroup = new BranchGroup();

	private BranchGroup physicsGroup;

	private BranchGroup visualGroup;

	private BranchGroup behaviourBranch;

	private NavigationTemporalBehaviour navigationTemporalBehaviour;

	private NavigationProcessorBullet navigationProcessor;

	private ICameraPanel cameraPanel;

	private AvatarLocation avatarLocation = new AvatarLocation();

	private AvatarCollisionInfo avatarCollisionInfo = new AvatarCollisionInfo(avatarLocation, 0.5f, 1.8f, 0.35f, 0.8f);

	private NavigationInputNewtKey keyNavigationInputNewt;

	private NavigationInputNewtMouseLocked newtMouseInputListener;

	private NewtJumpKeyListener jumpKeyListener;

	private NewtMiscKeyHandler newtMiscKeyHandler = new NewtMiscKeyHandler();

	private boolean showHavok = false;

	private boolean showVisual = true;

	private HUDFPSCounter fpsCounter;

	private HUDCompass hudcompass;

	private HUDCrossHair hudCrossHair;

	private HUDPosition hudPos;

	//private HUDPhysicsState hudPhysicsState;

	private HUDText firstInstruction;

	private HUDText loadInfo;
	private LoadingInfoBehavior loadingInfoBehavior;

	private PhysicsSystem physicsSystem;

	private ActionableMouseOverHandler cameraMouseOver;

	private AdminMouseOverHandler cameraAdminMouseOverHandler;

	private boolean freefly = false;

	private AmbientLight ambLight = null;

	private DirectionalLight dirLight = null;

	private GLWindow gl_window;

	//Can't use as threading causes massive trouble for scene loading
	//	private StructureUpdateBehavior structureUpdateBehavior;

	private NbccProvider nbccProvider = new NbccProvider() {
		@Override
		public NBControlledChar getNBControlledChar()
		{
			return physicsSystem.getNBControlledChar();
		}
	};

	public SimpleWalkSetup(String frameName)
	{
		this(frameName, null);
	}

	public SimpleWalkSetup(String frameName, GLWindow gl_window)
	{
		this.gl_window = gl_window;
		//kick off with a universe ***************************
		universe = new VisualPhysicalUniverse();

		//basic model and physics branch ************************
		modelGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		modelGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);

		physicsGroup = new BranchGroup();
		physicsGroup.setCapability(BranchGroup.ALLOW_DETACH);
		physicsGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		physicsGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		//modelGroup.addChild(physicsGroup); added if toggled on

		visualGroup = new BranchGroup();
		visualGroup.setCapability(BranchGroup.ALLOW_DETACH);
		visualGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		visualGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		visualGroup.setCapability(Group.ALLOW_PARENT_READ);
		modelGroup.addChild(visualGroup);

		universe.addToVisualBranch(modelGroup);
		behaviourBranch = new BranchGroup();
		behaviourBranch.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		behaviourBranch.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Create ambient light	and add it ************************
		float ambl = BethRenderSettings.getGlobalAmbLightLevel();
		Color3f alColor = new Color3f(ambl, ambl, ambl);
		ambLight = new AmbientLight(true, alColor);
		//ambLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		ambLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		ambLight.setCapability(Light.ALLOW_COLOR_WRITE);
		float dirl = BethRenderSettings.getGlobalDirLightLevel();
		Color3f dirColor = new Color3f(dirl, dirl, dirl);
		dirLight = new DirectionalLight(true, dirColor, new Vector3f(0f, -1f, 0f));
		//dirLight.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		dirLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		dirLight.setCapability(Light.ALLOW_COLOR_WRITE);
		BranchGroup lightsBG = new BranchGroup();
		lightsBG.addChild(ambLight);
		lightsBG.addChild(dirLight);
		universe.addToVisualBranch(lightsBG);

		// add the time keepers to the universe ************************

		//mouse/keyboard
		navigationTemporalBehaviour = new NavigationTemporalBehaviour();

		//jbullet
		navigationProcessor = new NavigationProcessorBullet(nbccProvider, avatarLocation);
		navigationTemporalBehaviour.addNavigationProcessor(navigationProcessor);
		behaviourBranch.addChild(navigationTemporalBehaviour);

		//add mouse and keyboard inputs ************************
		keyNavigationInputNewt = new NavigationInputNewtKey(navigationProcessor);
		NavigationInputNewtKey.VERTICAL_RATE = 50f;

		//mouseInputListener = new NavigationInputAWTMouseLocked();
		//mouseInputListener.setNavigationProcessor(navigationProcessor);
		newtMouseInputListener = new NavigationInputNewtMouseLocked();
		newtMouseInputListener.setNavigationProcessor(navigationProcessor);

		// dont' start mouse locked as its a pain
		//mouseInputListener.setCanvas(cameraPanel.getCanvas3D2D());

		//add jump key and vis/phy toggle key listeners for fun ************************
		jumpKeyListener = new NewtJumpKeyListener(nbccProvider);

		//some hud gear
		fpsCounter = new HUDFPSCounter();
		hudPos = new HUDPosition();
		hudcompass = new HUDCompass();
		//hudPhysicsState = new HUDPhysicsState();
		hudCrossHair = new HUDCrossHair();

		behaviourBranch.addChild(fpsCounter.getBehaviorBranchGroup());
		loadInfo = new HUDText(new Point2f(-0.95f, 0.1f), 18, "Loading...");
		loadingInfoBehavior = new LoadingInfoBehavior(loadInfo);
		behaviourBranch.addChild(loadingInfoBehavior);
		firstInstruction = new HUDText(new Point2f(-0.95f, 0f), 18, "Press tab to look around, press tab again to release mouse");

		//behaviourBranch.addChild(hudPhysicsState.getBehaviorBranchGroup());

		//	structureUpdateBehavior = new StructureUpdateBehavior();
		//	structureUpdateBehavior.setMaxElapsedTimeForCalls(20);
		//	behaviourBranch.addChild(structureUpdateBehavior);

		avatarLocation.addAvatarLocationListener(hudPos);
		avatarLocation.addAvatarLocationListener(hudcompass);

		//frame.setTitle(frameName);

		//frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		universe.addToBehaviorBranch(behaviourBranch);

		// Add a ShaderErrorListener
		universe.addShaderErrorListener(new ShaderErrorListener() {
			@Override
			public void errorOccurred(ShaderError error)
			{
				error.printVerbose();
				//JOptionPane.showMessageDialog(null, error.toString(), "ShaderError", JOptionPane.ERROR_MESSAGE);
			}
		});

		setupGraphicsSetting();
		cameraPanel.getCanvas3D2D().getGLWindow().setSize(1280, 720);//16:9 on this screen
		//cameraPanel.getCanvas3D2D().getGLWindow().setUndecorated(true);//better for demo
		cameraPanel.getCanvas3D2D().getGLWindow().setPosition(10, 10);
		cameraPanel.startRendering();
		cameraPanel.getCanvas3D2D().addNotify();

		/*
		
				simpleInventorySystem = new SimpleInventorySystem(fullScreenPanel3D);
				simpleInventorySystem.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentShown(ComponentEvent e)
					{
						setMouseLock(false);
					}
		
					@Override
					public void componentHidden(ComponentEvent e)
					{
						setMouseLock(true);
					}
				});*/

	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#closingTime()
	 */
	@Override
	public void closingTime()
	{
		if (cameraPanel != null)
			cameraPanel.stopRendering();
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getWindow()
	 */
	@Override
	public Window getWindow()
	{
		return cameraPanel.getCanvas3D2D().getGLWindow();
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#changeLocation(javax.vecmath.Quat4f, javax.vecmath.Vector3f)
	 */
	@Override
	public void changeLocation(Quat4f rot, Vector3f trans)
	{
		System.out.println("Moving to " + trans);
		//TODO: should I call warp now? not needed if only change cell uses the above
		warp(trans);
		getAvatarLocation().setTranslation(trans);
		getAvatarLocation().setRotation(rot);
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#warp(javax.vecmath.Vector3f)
	 */
	@Override
	public void warp(Vector3f origin)
	{
		if (physicsSystem != null && physicsSystem.getNBControlledChar() != null)
		{
			physicsSystem.getNBControlledChar().getCharacterController().warp(origin);
		}

	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setGlobalAmbLightLevel(float)
	 */
	@Override
	public void setGlobalAmbLightLevel(float f)
	{
		Color3f alColor = new Color3f(f, f, f);
		ambLight.setColor(alColor);
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setGlobalDirLightLevel(float)
	 */
	@Override
	public void setGlobalDirLightLevel(float f)
	{
		Color3f dirColor = new Color3f(f, f, f);
		dirLight.setColor(dirColor);
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#configure(utils.source.MeshSource, scrollsexplorer.simpleclient.SimpleBethCellManager)
	 */
	@Override
	public void configure(MeshSource meshSource, SimpleBethCellManager simpleBethCellManager)
	{
		// set up and run the physics system************************************************

		physicsSystem = new PhysicsSystem(simpleBethCellManager, avatarCollisionInfo, behaviourBranch, meshSource);

		IDashboard.dashboard.setPhysicSystem(physicsSystem);

		cameraMouseOver = new ActionableMouseOverHandler(physicsSystem, simpleBethCellManager);

		cameraAdminMouseOverHandler = new AdminMouseOverHandler(physicsSystem);

	}

	private void setupGraphicsSetting()
	{

		if (cameraPanel == null)
		{
			// must record start state to restore later
			boolean isLive = enabled;

			if (isLive)
			{
				setEnabled(false);
			}

			// clean any old gear
			/*	if (cameraPanel != null)
				{
					// reverse of construction below basically
					avatarLocation.removeAvatarLocationListener(cameraPanel.getDolly());
					cameraPanel.getCanvas3D2D().getHudShapeRoot().detach();
					Canvas3D2D canvas3D2D = cameraPanel.getCanvas3D2D();
					canvas3D2D.removeKeyListener(keyNavigationInputAWT);
					canvas3D2D.removeKeyListener(jumpKeyListener);
					canvas3D2D.removeKeyListener(miscKeyHandler);
					fpsCounter.removeFromCanvas(canvas3D2D);
					hudPos.removeFromCanvas(canvas3D2D);
					hudcompass.removeFromCanvas(canvas3D2D);
					hudPhysicsState.removeFromCanvas(canvas3D2D);
			
					frame.getContentPane().remove((JPanel) cameraPanel);
				}*/

			HMD_MODE = false;//gs.isOculusView();

			//create the camera panel ************************
			if (HMD_MODE)
			{
				System.out.println("HMD mode");
				try
				{
					cameraPanel = new HMDCameraPanel(universe);
					// and the dolly it rides on
					HMDCamDolly hcd = new HMDCamDolly(avatarCollisionInfo);
					cameraPanel.setDolly(hcd);

					//disable pitch in body
					navigationProcessor.setNoPitch(true);
					navigationTemporalBehaviour.addNavigationProcessor(hcd);
					cameraPanel.getCanvas3D2D().getGLWindow().addKeyListener(new HMDKeyHandler(hcd));
				}
				catch (OculusException e)
				{
					e.printStackTrace();
					//JOptionPane.showMessageDialog(null, "No Oculus or failure", "Oculus", JOptionPane.ERROR_MESSAGE);
				}

			}

			//if HMD fails or not HMD
			if (cameraPanel == null)
			{
				if (gl_window == null)
				{
					cameraPanel = new CameraPanel(universe);
					cameraPanel.getCanvas3D2D().getGLWindow().setPosition(600, 10);
				}
				else
				{
					cameraPanel = new CameraPanel(universe, gl_window);
				}

				// and the dolly it rides on
				if (TRAILER_CAM)
				{
					TrailerCamDolly trailerCamDolly = new TrailerCamDolly(avatarCollisionInfo, new WalkTrailorCamCollider());
					cameraPanel.setDolly(trailerCamDolly);
				}
				else
				{
					HeadCamDolly headCamDolly = new HeadCamDolly(avatarCollisionInfo);
					cameraPanel.setDolly(headCamDolly);
				}
			}

			//frame.getContentPane().add((JPanel) cameraPanel);

			avatarLocation.addAvatarLocationListener(cameraPanel.getDolly());
			cameraPanel.getDolly().locationUpdated(avatarLocation.get(new Quat4f()), avatarLocation.get(new Vector3f()));
			//cameraPanel.getDolly().setHudShape(cameraPanel.getCanvas3D2D().getHudShapeRoot());

			//			DDSTextureLoader.setAnisotropicFilterDegree(gs.getAnisotropicFilterDegree());
			//			cameraPanel.setSceneAntialiasingEnable(gs.isAaRequired());

			Canvas3D2D canvas3D2D = cameraPanel.getCanvas3D2D();
			canvas3D2D.getGLWindow().addKeyListener(keyNavigationInputNewt);
			canvas3D2D.getGLWindow().addKeyListener(jumpKeyListener);
			canvas3D2D.getGLWindow().addKeyListener(newtMiscKeyHandler);

			fpsCounter.addToCanvas(canvas3D2D);
			hudPos.addToCanvas(canvas3D2D);
			hudcompass.addToCanvas(canvas3D2D);
			//hudPhysicsState.addToCanvas(canvas3D2D);
			hudCrossHair.addToCanvas(canvas3D2D);
			loadInfo.addToCanvas(canvas3D2D);
			//allow tab for mouse lock
			//			canvas3D2D.setFocusTraversalKeysEnabled(false);

			//			canvas3D2D.addComponentListener(canvasResizeListener);

			if (isLive)
			{
				setEnabled(true);
			}
		}
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#resetGraphicsSetting()
	 */
	@Override
	public void resetGraphicsSetting()
	{
		/*		GraphicsSettings gs = ScreenResolution.organiseResolution(Preferences.userNodeForPackage(SimpleWalkSetup.class), frame, false,
						false, true, false);
				if (gs != null)
				{
					setupGraphicsSetting(gs);
				}*/
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enable)
	{
		if (enable != enabled)
		{
			System.out.println("Setting Enabled " + enable);
			// start the processor up ************************
			navigationProcessor.setActive(enable);
			if (enable)
			{
				cameraMouseOver.setConfig(cameraPanel.getCanvas3D2D());
				cameraAdminMouseOverHandler.setConfig(cameraPanel.getCanvas3D2D());
				physicsSystem.unpause();
				loadInfo.removeFromCanvas();
				loadingInfoBehavior.setEnable(false);
			}
			else
			{
				cameraMouseOver.setConfig(null);
				cameraAdminMouseOverHandler.setConfig(null);
				physicsSystem.pause();
				//frame.setVisible(false);
				//cameraPanel.stopRendering();// this kills the J3d stuff like removeNotify did
				loadInfo.addToCanvas(cameraPanel.getCanvas3D2D());
				loadingInfoBehavior.setEnable(true);
			}
			enabled = enable;
		}

	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setFreeFly(boolean)
	 */
	@Override
	public void setFreeFly(boolean ff)
	{
		if (physicsSystem.getNBControlledChar() != null)
		{
			physicsSystem.getNBControlledChar().getCharacterController().setFreeFly(ff);
		}
		keyNavigationInputNewt.setAllowVerticalMovement(ff);
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getPhysicsSystem()
	 */
	@Override
	public PhysicsSystem getPhysicsSystem()
	{
		return physicsSystem;
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getVisualBranch()
	 */
	@Override
	public BranchGroup getVisualBranch()
	{
		return visualGroup;
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getPhysicalBranch()
	 */
	@Override
	public BranchGroup getPhysicalBranch()
	{
		return physicsGroup;
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#toggleHavok()
	 */
	@Override
	public void toggleHavok()
	{
		showHavok = !showHavok;
		if (showHavok && physicsGroup.getParent() == null)
		{
			modelGroup.addChild(physicsGroup);
		}
		else if (!showHavok && physicsGroup.getParent() != null)
		{
			physicsGroup.detach();
		}
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#toggleVisual()
	 */
	@Override
	public void toggleVisual()
	{
		showVisual = !showVisual;
		if (showVisual && visualGroup.getParent() == null)
		{
			//Bad no good 
			//structureUpdateBehavior.add(modelGroup, visualGroup);
			modelGroup.addChild(visualGroup);
		}
		else if (!showVisual && visualGroup.getParent() != null)
		{
			//structureUpdateBehavior.remove(modelGroup, visualGroup);
			visualGroup.detach();
		}
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setVisualDisplayed(boolean)
	 */
	@Override
	public void setVisualDisplayed(boolean newShowVisual)
	{
		if (newShowVisual && visualGroup.getParent() == null)
		{
			//structureUpdateBehavior.add(modelGroup, visualGroup);
			modelGroup.addChild(visualGroup);

		}
		else if (!newShowVisual && visualGroup.getParent() != null)
		{
			//structureUpdateBehavior.remove(modelGroup, visualGroup);
			visualGroup.detach();
		}

		showVisual = newShowVisual;
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getAvatarLocation()
	 */
	@Override
	public AvatarLocation getAvatarLocation()
	{
		return avatarLocation;
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setPhysicsEnabled(boolean)
	 */
	@Override
	public void setPhysicsEnabled(boolean enable)
	{
		physicsSystem.getPhysicsLocaleDynamics().setSkipStepSim(!enable);
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getAvatarCollisionInfo()
	 */
	@Override
	public AvatarCollisionInfo getAvatarCollisionInfo()
	{
		return avatarCollisionInfo;
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#getViewingPlatform()
	 */
	@Override
	public ViewingPlatform getViewingPlatform()
	{
		// this won't work for the HMD version for now, as it it 2 platforms
		return (ViewingPlatform) cameraPanel.getDolly();
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setAzerty(boolean)
	 */
	@Override
	public void setAzerty(boolean a)
	{
		if (a)
		{
			NavigationInputNewtKey.FORWARD_KEY = KeyEvent.VK_Z;
			//NavigationInputAWTKey.FAST_KEY = KeyEvent.VK_E;
			//NavigationInputAWTKey.BACK_KEY = KeyEvent.VK_S;
			NavigationInputNewtKey.LEFT_KEY = KeyEvent.VK_Q;
			//NavigationInputAWTKey.RIGHT_KEY = KeyEvent.VK_D;
			NavigationInputNewtKey.UP_KEY = KeyEvent.VK_A;
			NavigationInputNewtKey.DOWN_KEY = KeyEvent.VK_W;
		}
		else
		{
			NavigationInputNewtKey.FORWARD_KEY = KeyEvent.VK_W;
			//NavigationInputAWTKey.FAST_KEY = KeyEvent.VK_E;
			//NavigationInputAWTKey.BACK_KEY = KeyEvent.VK_S;
			NavigationInputNewtKey.LEFT_KEY = KeyEvent.VK_A;
			//NavigationInputAWTKey.RIGHT_KEY = KeyEvent.VK_D;
			NavigationInputNewtKey.UP_KEY = KeyEvent.VK_Q;
			NavigationInputNewtKey.DOWN_KEY = KeyEvent.VK_Z;
		}
	}

	/* (non-Javadoc)
	 * @see scrollsexplorer.simpleclient.SimpleWalkSetupInterface#setMouseLock(boolean)
	 */
	@Override
	public void setMouseLock(boolean mouseLock)
	{
		if (!mouseLock)
		{
			//mouseInputListener.setCanvas(null);
			newtMouseInputListener.setWindow(null);

			//note tab message only put up if tab used to unlock mouse
		}
		else
		{
			//mouseInputListener.setCanvas(cameraPanel.getCanvas3D2D());
			newtMouseInputListener.setWindow(cameraPanel.getCanvas3D2D().getGLWindow());

			// always clear the tab message regardless
			if (firstInstruction != null)
			{
				firstInstruction.removeFromCanvas();
			}
		}
	}

	private class HMDKeyHandler implements KeyListener
	{
		private HMDCamDolly hcd;

		public HMDKeyHandler(HMDCamDolly hcd)
		{
			this.hcd = hcd;
			System.out.println("-,+ move eye dist");
			System.out.println("B reset oculus");
			System.out.println("F11 send output to oculus");
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_MINUS)
			{
				System.out.println("IPD-");
				hcd.changeIPD(0.95f);
			}
			else if (e.getKeyCode() == KeyEvent.VK_EQUALS)
			{
				System.out.println("IPD+");
				hcd.changeIPD(1.05f);
			}
			else if (e.getKeyCode() == KeyEvent.VK_B)
			{
				System.out.println("resetting Rift");
				hcd.reset();
			}
			else if (e.getKeyCode() == KeyEvent.VK_F11)
			{
				System.out.println("sending to Rift");
				hcd.sendToRift();
			}

		}

		@Override
		public void keyReleased(KeyEvent e)
		{

		}
	}

	private class NewtMiscKeyHandler implements KeyListener
	{
		public NewtMiscKeyHandler()
		{
			/*System.out.println("Esc exit");
			System.out.println("H toggle havok display");
			System.out.println("L toggle visual display");
			System.out.println("TAB toggle mouse lock");
			System.out.println("F toggle freefly");
			System.out.println("J display jbullet debug");*/
		}

		@Override
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				//System.out.println("Need a new exit dialog system");
				/*if (!exitDialogPane3D.isVisible())
				{
					//unlock mouse to interact
					setMouseLock(false);
					exitDialogPane3D.setVisible(true);
				}
				else
				{
					//hide dialog and lock mouse
					exitDialogPane3D.setVisible(false);
					setMouseLock(true);
				}*/
			}
			else if (e.getKeyCode() == KeyEvent.VK_H)
			{
				toggleHavok();
			}
			else if (e.getKeyCode() == KeyEvent.VK_L)
			{
				toggleVisual();
			}
			else if (e.getKeyCode() == KeyEvent.VK_F)
			{
				freefly = !freefly;
				setFreeFly(freefly);
			}
			else if (e.getKeyCode() == KeyEvent.VK_J)
			{
				physicsSystem.setDisplayDebug(true);
			}
			else if (e.getKeyCode() == KeyEvent.VK_TAB)
			{
				if (newtMouseInputListener.hasGLWindow())
				{
					setMouseLock(false);
					if (firstInstruction != null)
					{
						firstInstruction.addToCanvas(cameraPanel.getCanvas3D2D());
					}
				}
				else
				{
					setMouseLock(true);
				}

			}
			else if (e.getKeyCode() == KeyEvent.VK_I)
			{
				// simpleInventorySystem has a listener for the mouse lock
				System.out.println("Need a new inventory system");
			}
		}

		@Override
		public void keyReleased(KeyEvent arg0)
		{

		}
	}

	private class WalkTrailorCamCollider implements TrailorCamCollider
	{
		private Vector3f rayFrom = new Vector3f();

		private Vector3f rayTo = new Vector3f();

		@Override
		public float getCollisionFraction(Point3d lookAt, Vector3d cameraVector)
		{
			rayFrom.set(lookAt);
			//CAREFUL!!! 3d and 3f conversion requires non-trivial container usage!!!only the set method takes a 3d,
			//the add doesn't, so rayFrom is being used as a temp holder			
			rayTo.set(cameraVector);
			rayTo.add(rayFrom, rayTo);

			if (physicsSystem != null)
			{
				ClosestRayResultCallback crrc = physicsSystem.findRayIntersect(rayFrom, rayTo, -1);
				if (crrc != null)
				{
					return crrc.closestHitFraction;
				}
			}

			return 1f;
		}
	}

	@Override
	public boolean isTrailorCam()
	{	
		return TRAILER_CAM;
	}

}
