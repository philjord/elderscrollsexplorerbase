package scrollsexplorer;

import java.util.ArrayList;

import org.jogamp.vecmath.Vector3f;

import esmj3d.j3d.cell.J3dICellFactory;
import tools3d.utils.YawPitch;

public class GameConfig
{
	public static ArrayList<GameConfig> allGameConfigs = new ArrayList<GameConfig>();

	public String gameName;

	public J3dICellFactory j3dCellFactory;

	public String skyTexture;

	public String[] loadScreens;

	public float avatarYHeight;

	public String folderKey;

	public String scrollsFolder = null;

	public String mainESMFile;

	public String ftpFolderName;

	public int startCellId;

	public Vector3f startLocation;

	public YawPitch startYP;

	public GameConfig(String gameName, J3dICellFactory j3dCellFactory, String skyTexture, String[] loadScreens, float avatarYHeight,
			String folderKey, String mainESMFile, String ftpFolderName, int startCellId, Vector3f startLocation, YawPitch startYP)
	{
		this.gameName = gameName;
		this.j3dCellFactory = j3dCellFactory;
		this.skyTexture = skyTexture;
		this.loadScreens = loadScreens;
		this.avatarYHeight = avatarYHeight;
		this.folderKey = folderKey;
		this.ftpFolderName = ftpFolderName;
		this.mainESMFile = mainESMFile;
		this.startCellId = startCellId;
		this.startLocation = startLocation;
		this.startYP = startYP;
		update();
	}

	public void update()
	{
		this.scrollsFolder = PropertyLoader.properties.getProperty(folderKey);
	}

	public String getESMPath()
	{
		return scrollsFolder + PropertyLoader.fileSep + mainESMFile;
	}

	static
	{
		allGameConfigs.add(new GameConfig("TESIII: Morrowind", //
				new esmj3dtes3.j3d.cell.J3dCellFactory(), //
				"textures\\tx_sky_clear.dds", //
				new String[] { "meshes\\f\\act_banner_ald_velothi.nif" }, 
				//"meshes\\f\\act_banner_gnaar_mok.nif", //
				//"meshes\\f\\flora_tree_gl_07.nif", "meshes\\a\\towershield_glass.nif" }, //
				1.8f, //
				"MorrowindFolder", //
				"Morrowind.esm", //
				"morrowind", 0, new Vector3f(-108, 3, 936), new YawPitch()));

		allGameConfigs.add(new GameConfig("TESIV: Oblivion", //
				new esmj3dtes4.j3d.cell.J3dCellFactory(), //
				"textures\\sky\\cloudsclear.dds", //
				new String[] { "meshes\\furniture\\kvatch\\kvatchthrone01.nif", //
						"meshes\\armor\\cowlofthegrayfox\\helmet_gnd.nif" }, //
				1.8f, //
				"OblivionFolder", //
				"Oblivion.esm", //
				"oblivion", 186689, new Vector3f(57, 80, 0), new YawPitch()));

		//Android TESIV: Oblivion = 143176, (425,43,-912)

		allGameConfigs.add(new GameConfig("FO3: Fallout 3", //
				new esmj3dfo3.j3d.cell.J3dCellFactory(), //
				"textures\\sky\\urbancloudovercastlower01.dds", //
				new String[] { "meshes\\pipboy3000\\pipboyarmfemale.nif", //
						"meshes\\armor\\combatarmor\\m\\outfitm.nif" }, //
				1.8f, //
				"FallOut3Folder", //
				"Fallout3.esm", //
				"fallout3", 60, new Vector3f(-162, 182, 155), new YawPitch()));

		//Android FO3: Fallout 3 = 2676, (-37, 165, 281)

		allGameConfigs.add(new GameConfig("FONV: Fallout New Vegas", //
				new esmj3dfo3.j3d.cell.J3dCellFactory(), //
				"textures\\sky\\urbancloudovercastlower01.dds", //
				new String[] { "meshes\\furniture\\tlpod01.nif" }, //
				1.8f, //
				"FalloutNVFolder", //
				"FalloutNV.esm", //
				"falloutnv", 894758, new Vector3f(-927, 111, -16), new YawPitch()));
		//Android FONV: Fallout New Vegas = 1064441, (23, 94, -24)

		allGameConfigs.add(new GameConfig("TESV: Skyrim", //
				new esmj3dtes5.j3d.cell.J3dCellFactory(), //
				"textures\\sky\\skyrimcloudsupper04.dds", //
				new String[] { "meshes\\loadscreenart\\loadscreenadventure01.nif", // 						
						"meshes\\loadscreenart\\loadscreendraugrfemale.nif", //						
						"meshes\\loadscreenart\\loadscreengiant01.nif", //
						"meshes\\loadscreenart\\loadscreennoblebeddouble02.nif", //
						"meshes\\loadscreenart\\loadscreenswampdragon1.nif" }, //
				1.8f, //
				"SkyrimFolder", //
				"Skyrim.esm", //
				"skyrim", 60, new Vector3f(346, 256, 1459), new YawPitch()));
		//Android TESV: Skyrim = 107119, (251, -44, 94)

		allGameConfigs.add(new GameConfig("FO4: Fallout 4", //
				new esmj3dfo4.j3d.cell.J3dCellFactory(), //
				"textures\\sky\\skyrimcloudsupper04.dds", //hahhah!
				new String[] { "meshes\\LoadScreenArt\\Armor01PowerArmor1.nif", "meshes\\LoadScreenArt\\CreatureDeathclawRunForward.nif", //
						"meshes\\LoadScreenArt\\CryoPodLoadScreenArt01.nif", //
						"meshes\\LoadScreenArt\\LS_WorkstationArmorB01.nif", //
						"meshes\\LoadScreenArt\\VaultExitLoadScreen.nif" }, //
				1.8f, //
				"FallOut4Folder", //
				"Fallout4.esm", //
				"fallout4", 3988, new Vector3f(40, -18, -10), new YawPitch()));

		//Android FO4: Fallout 4 = 7768, (19, 1, 5)

		allGameConfigs.add(new GameConfig("Hunter Sneaker", //
				new esmj3dtes4.j3d.cell.J3dCellFactory(), //
				"textures\\sky\\cloudsclear.dds", //
				new String[] { "meshes\\architecture\\cloudrulertemple\\testcloudrulerint.nif" }, //
				1.8f, //
				"HunterSneakerFolder", //
				"OblivionHS.esm", //
				"huntersneaker", 60, new Vector3f(0, 200, 0), new YawPitch()));
	}
	//morrowind
	//meshes\i\in_v_arena.nif
	//meshes\i\in_de_shipwreckul_lg.nif
	//meshes\i\in_dae_hall_l_stair_curve_01.nif

	// obliv
	//meshes\dungeons\chargen\prisonhall04.nif
	//meshes\dungeons\cloudrulertemple\testcloudrulerint.nif
	//meshes\dungeons\cathedral\cathedralstenintback01.nif

	//FO3 + FONV
	//meshes\interface\loading\loadinganim01.nif

	//skyrim
	// all from meshes\loadscreenart  with black background

	//FO4
	// all from meshes\loadscreenart  with black background

	/**	
	 
	public void setSources(IESMManager esmManager, MediaSources mediaSources)
	{
	
		float version = esmManager.getVersion();
	
		if (version == 0.94f)
		{
			if (esmManager.getName().equals("Skyrim.esm"))
			{
				j3dCellFactory = new esmj3dtes5.j3d.cell.J3dCellFactory(esmManager, esmManager, mediaSources);
			}
			else
			{
	
				j3dCellFactory = new esmj3dfo3.j3d.cell.J3dCellFactory(esmManager, esmManager, mediaSources);
			}
		}
		else if (version == 1.32f)
		{
			j3dCellFactory = new esmj3dfo3.j3d.cell.J3dCellFactory(esmManager, esmManager, mediaSources);
		}
		else if (version == 1.0f || version == 0.8f)
		{
			j3dCellFactory = new esmj3dtes4.j3d.cell.J3dCellFactory(esmManager, esmManager, mediaSources);
		}
		else if (version == 1.2f)
		{
			j3dCellFactory = new esmj3dtes3.j3d.cell.J3dCellFactory(esmManager, esmManager, mediaSources);
		}
		else
		{
			System.out.println("Bad esm version! " + version + " in " + esmManager.getName());
		}
	
		//System.out.println("j3dCellFactory = " + j3dCellFactory);
	}*/
}
