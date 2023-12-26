package scrollsexplorer.simpleclient;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import esfilemanager.common.data.record.Record;
import esfilemanager.common.data.record.Subrecord;
import esmj3d.ai.AIActor;
import esmj3d.j3d.cell.J3dCELLGeneral;
import esmj3d.j3d.cell.J3dICELLPersistent;
import esmj3d.j3d.cell.J3dICellFactory;
import esmj3d.j3d.j3drecords.inst.J3dRECOChaInst;
import esmj3d.j3d.j3drecords.inst.J3dRECOInst;
import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import tools3d.utils.YawPitch;

public class BethInteriorVisualBranch extends BranchGroup
{

	private J3dICELLPersistent j3dCELLPersistent;

	private J3dCELLGeneral interiorCELLTemporary;

	private J3dCELLGeneral interiorCELLDistant;

	public BethInteriorVisualBranch(int interiorCellFormId, String cellFormName, J3dICellFactory j3dCellFactory,
			PhysicsSystem clientPhysicsSystem)
	{
		this.setName("BethInteriorVisualBranch" + cellFormName);

		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		j3dCELLPersistent = j3dCellFactory.makeBGInteriorCELLPersistent(interiorCellFormId, false);
		j3dCELLPersistent.getGridSpaces().updateAll();
		addChild((J3dCELLGeneral) j3dCELLPersistent);
		interiorCELLTemporary = j3dCellFactory.makeBGInteriorCELLTemporary(interiorCellFormId, false);
		addChild(interiorCELLTemporary);

		if (BethWorldVisualBranch.LOAD_PHYS_FROM_VIS)
		{
			clientPhysicsSystem.cellChanged(interiorCellFormId, (J3dCELLGeneral) j3dCELLPersistent);
			clientPhysicsSystem.loadJ3dCELL(interiorCELLTemporary);
		}

		interiorCELLDistant = j3dCellFactory.makeBGInteriorCELLDistant(interiorCellFormId, false);
		addChild(interiorCELLDistant);

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
		if (interiorCELLTemporary != null)
		{
			J3dRECOInst jri = interiorCELLTemporary.getJ3dRECOs().get(recoId);
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
		}
		
	}

	public J3dRECOChaInst getVisualActor(AIActor aiActor)
	{
		return (J3dRECOChaInst) getJ3dInstRECO(aiActor.getActorFormId());
	}

}
