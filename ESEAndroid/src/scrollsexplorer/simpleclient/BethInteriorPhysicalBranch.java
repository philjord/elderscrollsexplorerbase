package scrollsexplorer.simpleclient;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import esmj3d.ai.AIActor;
import esmj3d.j3d.cell.J3dCELLGeneral;
import esmj3d.j3d.cell.J3dICELLPersistent;
import esmj3d.j3d.cell.J3dICellFactory;
import esmj3d.j3d.j3drecords.inst.J3dRECOChaInst;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import esmmanager.common.data.record.Record;
import esmmanager.common.data.record.Subrecord;
import nifbullet.BulletNifModel;
import nifbullet.cha.NBNonControlledChar;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import tools3d.utils.YawPitch;

public class BethInteriorPhysicalBranch extends BranchGroup
{
	private J3dICELLPersistent j3dCELLPersistent;

	private J3dCELLGeneral j3dCELLTemporary;

	private PhysicsSystem clientPhysicsSystem;

	public BethInteriorPhysicalBranch(PhysicsSystem clientPhysicsSystem, int interiorCellFormId, J3dICellFactory j3dCellFactory)
	{
		this.setName("BethInteriorPhysicalBranch" + interiorCellFormId);
		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		this.clientPhysicsSystem = clientPhysicsSystem;

		j3dCELLPersistent = j3dCellFactory.makeBGInteriorCELLPersistent(interiorCellFormId, true);
		j3dCELLPersistent.getGridSpaces().updateAll();//force add all
		addChild((J3dCELLGeneral) j3dCELLPersistent);
		clientPhysicsSystem.cellChanged(interiorCellFormId, (J3dCELLGeneral) j3dCELLPersistent);

		j3dCELLTemporary = j3dCellFactory.makeBGInteriorCELLTemporary(interiorCellFormId, true);
		addChild(j3dCELLTemporary);
		clientPhysicsSystem.loadJ3dCELL(j3dCELLTemporary);

		//TODO: why the hell was I calling this???
		//addChild(j3dCellFactory.makeBGInteriorCELLDistant(interiorCellFormId, true));
		//not added to physics

	}

	public void handleRecordCreate(Record record)
	{
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().handleRecordCreate(record);
		}
	}

	public void handleRecordDelete(Record record)
	{
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().handleRecordDelete(record);
		}
	}

	public void handleRecordUpdate(Record record, Subrecord updatedSubrecord)
	{
		if (j3dCELLPersistent != null)
		{
			j3dCELLPersistent.getGridSpaces().handleRecordUpdate(record, updatedSubrecord);
		}

	}

	public J3dRECOInst getJ3dInstRECO(int recoId)
	{
		if (j3dCELLTemporary != null)
		{
			J3dRECOInst jri = j3dCELLTemporary.getJ3dRECOs().get(recoId);
			if (jri != null)
			{
				return jri;
			}
		}

		return j3dCELLPersistent.getGridSpaces().getJ3dInstRECO(recoId);
	}

	public void setLocationForActor(AIActor aiActor, Vector3f location, YawPitch yawPitch)
	{
		J3dRECOInst j3dRECOInst = getJ3dInstRECO(aiActor.getActorFormId());
		if (j3dRECOInst != null)
		{
			Quat4f q = new Quat4f();
			yawPitch.get(q);
			j3dRECOInst.setLocation(new Vector3f(location.x, location.y, location.z), q);
			BulletNifModel nbm = clientPhysicsSystem.getNifBullet(aiActor.getActorFormId());
			if (nbm instanceof NBNonControlledChar)
			{
				NBNonControlledChar ncc = (NBNonControlledChar) nbm;
				ncc.setTransform(q, location);
			}
			else
			{
				System.out.println("setting location for non actor!! " + nbm);
			}
		}

	}

	public J3dRECOChaInst getVisualActor(AIActor aiActor)
	{
		return (J3dRECOChaInst) getJ3dInstRECO(aiActor.getActorFormId());
	}
}
