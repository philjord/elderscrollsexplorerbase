package scrollsexplorer.simpleclient.physics;

import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import esmj3d.j3d.j3drecords.inst.J3dRECOInst;

public interface InstRECOStore
{
	public void applyUpdate(J3dRECOInst instReco, Quat4f newRotation, Vector3f newTranslation);
}
