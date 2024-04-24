package scrollsexplorer;

import java.util.ArrayList;

import org.jogamp.vecmath.Vector3f;

import esmj3d.j3d.cell.J3dICellFactory;
import tools3d.utils.YawPitch;

public class GameConfig {
	public static ArrayList<GameConfig>	allGameConfigs	= new ArrayList<GameConfig>();

	public String						gameName;

	public J3dICellFactory				j3dCellFactory;

	public String						skyTexture;

	public String[]						loadScreens;

	public float						avatarYHeight;

	public String						folderKey;

	public String						scrollsFolder	= null;

	public String						mainESMFile;

	public String						ftpFolderName;

	public int							startCellId;
	
	public int							musicToPlayId; 

	public Vector3f						startLocation;

	public YawPitch						startYP;

	public GameConfig(	String gameName, J3dICellFactory j3dCellFactory, String skyTexture, String[] loadScreens,
						float avatarYHeight, String folderKey, String mainESMFile, String ftpFolderName,
						int startCellId, Vector3f startLocation, YawPitch startYP) {
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

	public void update() {
		this.scrollsFolder = PropertyLoader.properties.getProperty(folderKey);
	}

	public String getESMPath() {
		return scrollsFolder + PropertyLoader.fileSep + mainESMFile;
	}

	public static void init() {
		// only init once!
		if (allGameConfigs.size() == 0) {
			allGameConfigs.add(new GameConfig("TESIII: Morrowind", //
					new esmj3dtes3.j3d.cell.J3dCellFactory(), //
					"textures\\tx_sky_clear.dds", //
					new String[] {"meshes\\f\\act_banner_ald_velothi.nif",
						"meshes\\f\\act_banner_gnaar_mok.nif","meshes\\f\\act_banner_hla_oad.nif","meshes\\f\\act_banner_gnaar_mok.nif",
						"meshes\\f\\act_banner_khull.nif","meshes\\f\\act_banner_sadrith_mora.nif","meshes\\f\\act_banner_tel_aruhn.nif",
						"meshes\\f\\act_banner_tel_branora.nif","meshes\\f\\act_banner_tel_fyr.nif","meshes\\f\\act_banner_tel_mora.nif",
						"meshes\\f\\act_banner_tel_vos.nif","meshes\\f\\act_banner_vos.nif","meshes\\f\\furn_banner_dagoth_01.nif"}, 
					1.8f, //
					"MorrowindFolder", //
					"Morrowind.esm", //
					"morrowind", 0, new Vector3f(-108, 3, 936), new YawPitch()));

			allGameConfigs.add(new GameConfig("TESIV: Oblivion", //
					new esmj3dtes4.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\cloudsclear.dds", //
					new String[] {"meshes\\furniture\\kvatch\\kvatchthrone01.nif", //
						"meshes\\armor\\cowlofthegrayfox\\helmet_gnd.nif",
						"meshes\\menus\\spell effect timer\\timer.nif",
						"meshes\\dungeons\\chargen\\prisoncell01.nif"}, //
					1.8f, //
					"OblivionFolder", //
					"Oblivion.esm", //
					"oblivion", 186689, new Vector3f(57, 80, 0), new YawPitch()));

			allGameConfigs.add(new GameConfig("FO3: Fallout 3", //
					new esmj3dfo3.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\urbancloudovercastlower01.dds", //
					new String[] {"meshes\\pipboy3000\\pipboyarmfemale.nif", //
						"meshes\\armor\\combatarmor\\m\\outfitm.nif",
						"meshes\\interface\\loading\\loadinganim01.nif"}, //
					1.8f, //
					"FallOut3Folder", //
					"Fallout3.esm", //
					"fallout3", 60, new Vector3f(-162, 182, 155), new YawPitch()));

			allGameConfigs.add(new GameConfig("FONV: Fallout New Vegas", //
					new esmj3dfo3.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\urbancloudovercastlower01.dds", //
					new String[] {"meshes\\furniture\\tlpod01.nif",
						"meshes\\interface\\loading\\loadinganim01.nif"}, //
					1.8f, //
					"FalloutNVFolder", //
					"FalloutNV.esm", //
					"falloutnv", 894758, new Vector3f(-927, 111, -16), new YawPitch()));
			//Android FONV: Fallout New Vegas = 1064441, (23, 94, -24)

			allGameConfigs.add(new GameConfig("TESV: Skyrim", //
					new esmj3dtes5.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\skyrimcloudsupper04.dds", //
					new String[] {"meshes\\loadscreenart\\loadscreenadventure01.nif", // 						
						"meshes\\loadscreenart\\loadscreendraugrfemale.nif", //						
						"meshes\\loadscreenart\\loadscreengiant01.nif", //
						"meshes\\loadscreenart\\loadscreennoblebeddouble02.nif", //
						"meshes\\loadscreenart\\loadscreenswampdragon1.nif"}, //
					//FIXME, allow for a folder only and grab any of the nifs in there
					1.8f, //
					"SkyrimFolder", //
					"Skyrim.esm", //
					"skyrim", 60, new Vector3f(346, 256, 1459), new YawPitch()));//FIXME: default location is not useful, possibly a intro point

			allGameConfigs.add(new GameConfig("FO4: Fallout 4", //
					new esmj3dfo4.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\skyrimcloudsupper04.dds", //hahhah!
					new String[] {"meshes\\LoadScreenArt\\Armor01PowerArmor1.nif",
						"meshes\\LoadScreenArt\\CreatureDeathclawRunForward.nif", //
						"meshes\\LoadScreenArt\\CryoPodLoadScreenArt01.nif", //
						"meshes\\LoadScreenArt\\LS_WorkstationArmorB01.nif", //
						"meshes\\LoadScreenArt\\VaultExitLoadScreen.nif"}, //					
					1.8f, //
					"FallOut4Folder", //
					"Fallout4.esm", //
					"fallout4", 3988, new Vector3f(40, -18, -10), new YawPitch()));
			
			allGameConfigs.add(new GameConfig("FO76: Fallout 76", //
					new esmj3dfo76.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\skyrimcloudsupper04.dds", 
					new String[] {"meshes\\LoadScreenArt\\Armor01PowerArmor1.nif", }, //					
					1.8f, //
					"FallOut76Folder", //
					"SeventySix.esm", //
					"fallout76", // 2480661 main world 
					5930030//5930030 FortAtlasDungeon01
					, new Vector3f(40, -18, -10), new YawPitch()));

			allGameConfigs.add(new GameConfig("STAR: Starfield", //
					new esmj3dstar.j3d.cell.J3dCellFactory(), //FIXME
					"textures\\sky\\skyrimcloudsupper04.dds", 
					new String[] {"meshes\\LoadScreenArt\\Armor01PowerArmor1.nif" }, //					
					1.8f, //
					"StarfieldFolder", //
					"Starfield.esm", //
					"starfield", 3988, new Vector3f(40, -18, -10), new YawPitch()));

			allGameConfigs.add(new GameConfig("Hunter Sneaker", //
					new esmj3dtes4.j3d.cell.J3dCellFactory(), //
					"textures\\sky\\cloudsclear.dds", //
					new String[] {"meshes\\architecture\\cloudrulertemple\\testcloudrulerint.nif"}, //
					1.8f, //
					"HunterSneakerFolder", //
					"OblivionHS.esm", //
					"huntersneaker", 60, new Vector3f(0, 200, 0), new YawPitch()));
		}
	}
}
