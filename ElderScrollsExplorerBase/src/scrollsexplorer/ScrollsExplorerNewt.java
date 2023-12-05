package scrollsexplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.compressedtexture.CompressedTextureLoader;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

import bsa.source.BsaMeshSource;
import bsa.source.BsaSoundSource;
import bsa.source.BsaTextureSource;
import bsa.source.DDSToKTXBsaConverter;
import bsaio.ArchiveFile;
import bsaio.BSArchiveSetFile;
import bsaio.DBException;
import esmio.common.data.plugin.PluginGroup;
import esmio.common.data.record.Record;
import esmio.loader.ESMManager;
import esmio.loader.ESMManagerFile;
import esmio.loader.IESMManager;
import esmio.utils.ESMUtils;
import esmio.utils.source.EsmSoundKeyToName;
import esmj3d.data.shared.records.CommonREFR;
import esmj3d.data.shared.subrecords.XTEL;
import esmj3d.j3d.BethRenderSettings;
import esmj3d.j3d.cell.J3dICellFactory;
import esmj3d.j3d.j3drecords.inst.J3dLAND;
import javaawt.VMEventQueue;
import javaawt.image.VMBufferedImage;
import javaawt.imageio.VMImageIO;
import nativeLinker.LWJGLLinker;
import nif.BgsmSource;
import nif.appearance.NiGeometryAppearanceFactoryShader;
import nif.character.NifCharacter;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.particles.tes3.J3dNiParticles;
import nif.shader.ShaderSourceIO;
import scrollsexplorer.simpleclient.BethWorldVisualBranch;
import scrollsexplorer.simpleclient.SimpleBethCellManager;
import scrollsexplorer.simpleclient.SimpleWalkSetup;
import scrollsexplorer.simpleclient.SimpleWalkSetupInterface;
import scrollsexplorer.simpleclient.mouseover.ActionableMouseOverHandler;
import scrollsexplorer.simpleclient.physics.DynamicsEngine;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import scrollsexplorer.simpleclient.tes3.Tes3Extensions;
import tools.io.ConfigLoader;
import tools.io.FileChannelRAF;
import tools3d.camera.Camera;
import tools3d.utils.YawPitch;
import tools3d.utils.loader.PropertyCodec;
import tools3d.utils.scenegraph.LocationUpdateListener;
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

	public BSArchiveSetFile bsaFileSet;

	private GameConfig gameConfigToLoad = null;

	private Tes3Extensions tes3Extensions;

	private boolean stayAlive = true;

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

		//TODO this client should act EXACTLY like the phone one including find doors and the graphics config set up
		// of ScrollExplorer
		// also it should use a seperate bsa collection to ensure it does what the phoen does
		// these 5 test the "no dds support" issue and solution on phones
		BsaTextureSource.allowedTextureFormats = BsaTextureSource.AllowedTextureFormats.DDS;
		CompressedTextureLoader.RETURN_DECOMPRESSED_DDS = true;
		CompressedTextureLoader.DROP_0_MIP = true;
		javaawt.image.BufferedImage.installBufferedImageDelegate(VMBufferedImage.class);
		javaawt.imageio.ImageIO.installBufferedImageImpl(VMImageIO.class);
		javaawt.EventQueue.installEventQueueImpl(VMEventQueue.class);		

 
		
		BethWorldVisualBranch.LOAD_PHYS_FROM_VIS = false;
		DynamicsEngine.MAX_SUB_STEPS = 2;
		PhysicsSystem.MIN_TIME_BETWEEN_STEPS_MS = 40;
		NiGeometryAppearanceFactoryShader.setAsDefault();
		ShaderSourceIO.ES_SHADERS = false;// not the same!
		J3dNiTriBasedGeom.USE_FIXED_BOUNDS = true;
		// this definitely doesn't help on desktop, but lots of methods calls so maybe?
		NifCharacter.BULK_BUFFER_UPDATES = false;

		NiGeometryAppearanceFactoryShader.setAsDefault();
		CompressedTextureLoader.setAnisotropicFilterDegree(4);

 

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

		// so there is a lack of non daemon threads see jogamp.newt.driver.awt.AWTEDTUtil for example
		// so with a pure Newt world I have to keep the app alive with my own non daemon useless keep alive thread!
		// closing time has to kill it
		// the real solution is to find out why jogl doesn't provide a non daemon EDT thread for GLWindow seems strange
		// perhaps RAISEBUG:

		Thread newtKeepAliveThread = new Thread() {
			@Override
			public void run()
			{
				while (stayAlive)
				{
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		};
		newtKeepAliveThread.setDaemon(false);// in case a daemon parent
		newtKeepAliveThread.setName("Newt Keep Alive Thread");
		newtKeepAliveThread.start();
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

		
		// now to allow the app to exit 
		stayAlive = false;
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
		gameConfigToLoad = newGameConfig;
		simpleWalkSetup.getAvatarCollisionInfo().setAvatarYHeight(gameConfigToLoad.avatarYHeight);

		Thread t = new Thread() {
			@Override
			public void run()
			{
				synchronized (gameConfigToLoad)
				{
					IDashboard.dashboard.setEsmLoading(1);

					esmManager = ESMManagerFile.getESMManager(gameConfigToLoad.getESMPath());
					
					
					bsaFileSet = null;
					if (esmManager != null)
					{

						//TODO: all these should be connected strongly to GameConfig
						if (esmManager.getName().indexOf("Morrowind") != -1)
						{
							J3dLAND.setTes3();
							BethRenderSettings.setTes3(true);
						}

						YawPitch yp = YawPitch
								.parse(PropertyLoader.properties.getProperty("YawPitch" + esmManager.getName(), new YawPitch().toString()));
						Vector3f trans = PropertyCodec.vector3fOut(PropertyLoader.properties.getProperty("Trans" + esmManager.getName(),
								gameConfigToLoad.startLocation.toString()));
						int prevCellformid = Integer.parseInt(PropertyLoader.properties.getProperty("CellId" + esmManager.getName(), "-1"));
						simpleWalkSetup.getAvatarLocation().set(yp.get(new Quat4f()), trans);

						if (prevCellformid == -1)
						{
							prevCellformid = gameConfigToLoad.startCellId;
						}

						new EsmSoundKeyToName(esmManager);
						MeshSource meshSource;
						TextureSource textureSource;
						SoundSource soundSource;

						if (bsaFileSet == null)
						{
							bsaFileSet = new BSArchiveSetFile(new String[] { gameConfigToLoad.scrollsFolder }, true);
							//OK time to check that each bsa file that holds dds has a ktx equivilent and drop the dds version
							// or if not to convert the dds to ktx then drop the dds version
							
							//a list of new name/old dds archive pair so old can be taken out after new is found or created
							HashMap<String, ArchiveFile> neededBsas = new HashMap<String, ArchiveFile>();
							
							for (ArchiveFile archiveFile : bsaFileSet) {
								if (archiveFile != null && archiveFile.hasDDS()) {
									// we want a archive with the same name but _ktx before the extension holding KTX files
									String ddsArchiveName = archiveFile.getName();
									String ext = ddsArchiveName.substring(ddsArchiveName.lastIndexOf("."));
									String ktxArchiveName = ddsArchiveName.substring(0,ddsArchiveName.lastIndexOf("."));
									ktxArchiveName = ktxArchiveName + "_ktx" + ext;
									neededBsas.put(ktxArchiveName, archiveFile);
								}
							}
							for(String ktxArchiveName : neededBsas.keySet()) {
								ArchiveFile ddsArchive = neededBsas.get(ktxArchiveName);
								//remove the dds version archive either way
								try {
									ddsArchive.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
								bsaFileSet.remove(ddsArchive);
								
								boolean found = false;
								for (ArchiveFile archiveFile : bsaFileSet) {
									//TODO: should see  if it's got ktx in it, but for now let's just prey
									if (archiveFile != null && archiveFile.getName().equals(ktxArchiveName)) {
										found = true; 
										break;
									}
								}
								
								if(!found) {
									System.out.println("I should create a _ktx file about now " + ktxArchiveName );
									
									// I need the displayable version to convert so let's load a new copy
									File ddsfile = new File(gameConfigToLoad.scrollsFolder, ddsArchive.getName());
									FileInputStream fis;
									try {
										fis = new FileInputStream(ddsfile);
										
										long tstart = System.currentTimeMillis();
										System.out.println("Reloading as a displayable " + ddsfile.getPath());
										ArchiveFile archiveFile = ArchiveFile.createArchiveFile(fis.getChannel(), ddsfile.getName());
										archiveFile.load(true);
										System.out.println("loaded as a displayable " + ddsfile.getPath()  + " in " + (System.currentTimeMillis() - tstart));
										//convert convert
										tstart = System.currentTimeMillis();										
										File ktxfile = new File(gameConfigToLoad.scrollsFolder, ktxArchiveName);
										if (ktxfile.exists() && !ktxfile.delete())
											throw new IOException("Unable to delete '" + ktxfile.getPath() + "'");
										else {
											FileChannel fco = new java.io.RandomAccessFile(ktxfile, "rw").getChannel();
											DDSToKTXBsaConverter convert = new DDSToKTXBsaConverter(fco, archiveFile);
											System.out.println("converting to " + ktxfile.getPath());
											convert.start();
											try {
												convert.join();
											} catch (InterruptedException e) {
												e.printStackTrace();
											}									
											System.out.println(""	+ (System.currentTimeMillis() - tstart) + "ms to compress " + ktxfile.getPath());
									
											// now load that newly created file into the system
											bsaFileSet.loadFileAndWait(new FileInputStream(ktxfile).getChannel(), ktxfile.getName());
										}
									
									} catch (FileNotFoundException e) {
										e.printStackTrace();
									} catch (DBException e1) {
										e1.printStackTrace();
									} catch (IOException e1) {
										e1.printStackTrace();
									}								
								}								
							}						
						}

						//TODO: Morrowind appears to have sound and music as a seperate gosh darned file system system! not in a bsa
						
						
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
						
						
						 // do we have some ktx images or are we stuck with slow big dds decompress
                        boolean ddsDecompressing = !((BsaTextureSource) textureSource).hasKTX() && !((BsaTextureSource) textureSource).hasASTC();
                        // this is terrible 1/4 images, but helps if no KTX files are available and we are decompressing dds
                        if (ddsDecompressing)
                            CompressedTextureLoader.DROP_0_MIP = true;

                        if (gameConfigToLoad.folderKey.equals("MorrowindFolder")) {

                            BethRenderSettings.setFarLoadGridCount(8);
                            BethRenderSettings.setNearLoadGridCount(2);
                            BethRenderSettings.setLOD_LOAD_DIST_MAX(32);
                            BethRenderSettings.setObjectFade(150);
                            BethRenderSettings.setItemFade(120);
                            BethRenderSettings.setActorFade(50);
                            BethRenderSettings.setFogEnabled(false);
                            //BethWorldVisualBranch.FOG_START = 100;
                            //BethWorldVisualBranch.FOG_END = 250;

                            //long distance view
                            //BethRenderSettings.setFarLoadGridCount(16);
                            //
                        } else {

                            //TODO: must make a per game setting recorder for this gear!

                            //oblivion goes hard, others are cautious for now
                            if (gameConfigToLoad.folderKey.equals("OblivionFolder")) {
                                BethRenderSettings.setFarLoadGridCount(4);
                                BethRenderSettings.setNearLoadGridCount(1);
                                BethRenderSettings.setLOD_LOAD_DIST_MAX(32);
                                BethRenderSettings.setObjectFade(100);
                                BethRenderSettings.setItemFade(80);
                                BethRenderSettings.setActorFade(35);
                            }  else  {
                                BethRenderSettings.setFarLoadGridCount(3);
                                BethRenderSettings.setNearLoadGridCount(1);
                                BethRenderSettings.setLOD_LOAD_DIST_MAX(16);
                                BethRenderSettings.setObjectFade(50);
                                BethRenderSettings.setItemFade(60);
                                BethRenderSettings.setActorFade(35);
                                BethRenderSettings.setFogEnabled(false);
                            }
                        }

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
								J3dNiParticles.setScreenWidth(simpleWalkSetup.getWindow().getWidth());
							}

						});
						J3dNiParticles.setScreenWidth(simpleWalkSetup.getWindow().getWidth());
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
						simpleBethCellManager.setSources(gameConfigToLoad, esmManager, mediaSources);

						if (gameConfigToLoad == GameConfig.allGameConfigs.get(0))
						{
							System.out.println("Adding Tes3 extensions");
							tes3Extensions = new Tes3Extensions(gameConfigToLoad, esmManager, mediaSources, simpleWalkSetup,
									simpleBethCellManager);
						}

						
                        // need to set Trans to a door somewhere?
                        findADoor(prevCellformid, yp, trans);
                        
                        simpleWalkSetup.getAvatarLocation().set(yp.get(new Quat4f()), trans);
                               
                        
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
		System.setProperty("sun.awt.nopixfmt", "true");//for start up speed?
		System.setProperty("sun.awt.noerasebackground", "true");
		System.setProperty("j3d.cacheAutoComputeBounds", "true");
		System.setProperty("j3d.defaultReadCapability", "false");
		System.setProperty("j3d.defaultNodePickable", "false");
		System.setProperty("j3d.defaultNodeCollidable", "false");

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
	
	
	private void findADoor(int formToLoad, YawPitch yp, Vector3f trans) {
        J3dICellFactory j3dCellFactory = gameConfigToLoad.j3dCellFactory;
        if (j3dCellFactory != null) {

            ArrayList<CommonREFR> doors = new ArrayList<>();
            if (gameConfigToLoad.gameName != "TESIII: Morrowind") {
                // if SimpleBethCellManager.setSources has been called the persistent children will have been loaded
                PluginGroup cellChildGroups = j3dCellFactory.getPersistentChildrenOfCell(formToLoad);
                for (Record record : cellChildGroups.getRecordList()) {
                    // is this a door way?
                    if (record.getRecordType().equals("REFR")) {
                        // don't go game specific just the common data needed (which include XTEL!)
                        CommonREFR commonREFR = new CommonREFR(record, true);
                        XTEL xtel = commonREFR.XTEL;
                        //if we are a door outward we have a door inward
                        if (xtel != null) {
                            Record otherDoor;
                            if (xtel.doorFormId != 0) {
                                otherDoor = j3dCellFactory.getRecord(xtel.doorFormId);
                                if (otherDoor != null) {
                                    CommonREFR otherDoorCommonREFR = new CommonREFR(otherDoor, true);
                                    doors.add(otherDoorCommonREFR);
                                }
                            }
                        }

                    }
                }
            } else {
                if (formToLoad == 0) {
                    int attempts = 0;
                    while (doors.size() == 0 && attempts < 20) {
                        attempts++;
                        // morrowind itself we'll have to pick a random cell from -25 to +25 until we find a door
                        ///	looks like x = 23 to -18 y is 27 to -17
                        int x = (int) ((Math.random() * 41) - 18);
                        int y = (int) ((Math.random() * 44) - 17);
                        try {
                            PluginGroup cellChildGroups = esmManager.getWRLDExtBlockCELLChildren(0, x, y);
                            if (cellChildGroups != null) {
                                for (Record record : ESMUtils.getChildren(cellChildGroups, PluginGroup.CELL_TEMPORARY)) {
                                    // is this a door way?
                                    if (record.getRecordType().equals("REFR")) {
                                        //TODO: Fro Morrowind I need to find all doors elsewhere and see where they wouldput you in this cell
                                        // which is obviously too much work!

                                        // morrowind has a half pie system using DNAM
                                        // morrowind has no match inwards door so we'll have to make up t and yp
                                        esmj3dtes3.data.records.REFR commonREFR = new esmj3dtes3.data.records.REFR(record);
                                        XTEL xtel = commonREFR.XTEL;// xtel describe the target position and DNAM states the cell, but a NULL DNAM is Morrowind (id=0)
                                        if (xtel != null)
                                            doors.add(commonREFR);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }

                } else {
                    // no persistent system, just load them all!
                    PluginGroup cellChildGroups = j3dCellFactory.getPersistentChildrenOfCell(formToLoad);
                    if (cellChildGroups != null) {
                        for (Record record : ESMUtils.getChildren(cellChildGroups, PluginGroup.CELL_TEMPORARY)) {
                            // is this a door way?
                            if (record.getRecordType().equals("REFR")) {
                                //TODO: Fro Morrowind I need to find all doors elsewhere and see where they wouldput you in this cell
                                // which is obviously too much work!

                                // morrowind has a half pie system using DNAM
                                // morrowind has no match inwards door so we'll have to make up t and yp
                                esmj3dtes3.data.records.REFR commonREFR = new esmj3dtes3.data.records.REFR(record);
                                XTEL xtel = commonREFR.XTEL;// xtel describe the target position and DNAM states the cell, but a NULL DNAM is Morrowind (id=0)
                                if (xtel != null)
                                    doors.add(commonREFR);
                            }
                        }
                    }
                }

            }

            if (doors.size() > 0) {
                int idx = (int) (Math.random() * (doors.size() - 1));
                if (gameConfigToLoad.gameName != "TESIII: Morrowind") {
                    XTEL xtel = doors.get(idx).XTEL; // note this is the other door so the exit is right but it's cell is not our cell
                    Vector3f t = ActionableMouseOverHandler.getTrans(xtel.x, xtel.y, xtel.z);
                    t.y += 1; // TODO: cos it's the floor I reckon, nay something off in all direction a bit here
                    Quat4f r = ActionableMouseOverHandler.getRot(xtel.rx, xtel.ry, xtel.rz);




                    trans.set(t);
                    yp.set(r);
                } else {
                    esmj3dtes3.data.records.REFR refr = (esmj3dtes3.data.records.REFR) doors.get(idx);
                    Vector3f loc = refr.getTrans();
                    Vector3f rot = refr.getEulerRot();
                    //TODO:  location needs to be pushed forward in facing as this is just the door itself
                    Vector3f t = ActionableMouseOverHandler.getTrans(loc.x, loc.y, loc.z);
                    //t.y += 1; // TODO: cos it's the floor I reckon, nay something off in all direction a bit here
                    Quat4f r = ActionableMouseOverHandler.getRot(rot.x, rot.y, rot.z);


                    // now push forward by 1 meter to see if we are in front of the door
                    Transform3D t3d = new Transform3D(r,new Vector3f(0,0,0), 1f);
                    Vector3f f = new Vector3f(0,0,1);
                    t3d.transform(f);
                    t.add(f);

                    trans.set(t);
                    yp.set(r);
                }
            }
        }

    }

}
