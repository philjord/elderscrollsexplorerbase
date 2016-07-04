package scrollsexplorer.simpleclient.tes3;

import java.util.ArrayList;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import esmj3dtes3.character.CharacterSheet;
import esmj3dtes3.data.records.NPCO;
import esmj3dtes3.j3d.character.AvatarFirstPerson;
import esmmanager.loader.IESMManager;
import scrollsexplorer.GameConfig;
import scrollsexplorer.simpleclient.SimpleBethCellManager;
import scrollsexplorer.simpleclient.SimpleWalkSetupInterface;
import utils.source.MediaSources;

public class Tes3Extensions
{
	private GameConfig selectedGameConfig;
	private IESMManager esmManager;
	private MediaSources mediaSources;
	private SimpleWalkSetupInterface simpleWalkSetup;
	private SimpleBethCellManager simpleBethCellManager;

	private CharacterSheet characterSheet;
	private AvatarFirstPerson avatarFirstPerson;

	public Tes3Extensions(GameConfig selectedGameConfig, IESMManager esmManager, MediaSources mediaSources,
			SimpleWalkSetupInterface simpleWalkSetup, SimpleBethCellManager simpleBethCellManager)
	{
		this.selectedGameConfig = selectedGameConfig;
		this.esmManager = esmManager;
		this.mediaSources = mediaSources;
		this.simpleWalkSetup = simpleWalkSetup;
		this.simpleBethCellManager = simpleBethCellManager;

		ArrayList<NPCO> npcos = new ArrayList<NPCO>();

		//npcos.add(new NPCO(1, "bonemold_bracer_left"));
		//npcos.add(new NPCO(1, "bonemold_bracer_right"));
		//npcos.add(new NPCO(1, "dragonscale_towershield"));
		npcos.add(new NPCO(1, "steel battle axe"));

		characterSheet = new CharacterSheet(false, "Breton", "b\\b_n_breton_f_head_01.nif", "b\\b_n_breton_f_hair_01.nif", npcos);
		avatarFirstPerson = new AvatarFirstPerson(characterSheet, esmManager, mediaSources);

		TransformGroup tg = new TransformGroup();
		Transform3D t = new Transform3D();
		t.rotY(Math.PI / 2f);
		t.setTranslation(new Vector3f(0, -1.65f, -0.0f));
		//t.rotY(Math.PI / -2f);
		//t.setTranslation(new Vector3f(0, 0, -3f));
		tg.setTransform(t);
		tg.addChild(avatarFirstPerson);

		BranchGroup bg = new BranchGroup();
		bg.addChild(tg);

		simpleWalkSetup.getViewingPlatform().getPlatformGeometry().addChild(bg);

		avatarFirstPerson.playAnimation("idlespell", true);
		
		//idle1h
		//idle2c
		//idle2w
		//idlehh
		//idlecrossbow
		//idlespell
		//idlestorm
		//idle
		
	}

}
