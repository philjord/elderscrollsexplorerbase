package scrollsexplorer.simpleclient;

import javax.media.j3d.BranchGroup;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.jogamp.newt.Window;
import com.sun.j3d.utils.universe.ViewingPlatform;

import scrollsexplorer.simpleclient.physics.PhysicsSystem;
import tools3d.navigation.AvatarCollisionInfo;
import tools3d.navigation.AvatarLocation;
import utils.source.MeshSource;

public interface SimpleWalkSetupInterface
{

	void closingTime();

	/**
	 * Only for listening to shutdown
	 * @return
	 */
	Window getWindow();

	void changeLocation(Quat4f rot, Vector3f trans);

	void warp(Vector3f origin);

	void setGlobalAmbLightLevel(float f);

	void setGlobalDirLightLevel(float f);

	void configure(MeshSource meshSource, SimpleBethCellManager simpleBethCellManager);

	void resetGraphicsSetting();

	void setEnabled(boolean enable);

	void setFreeFly(boolean ff);

	PhysicsSystem getPhysicsSystem();

	BranchGroup getVisualBranch();

	BranchGroup getPhysicalBranch();

	void toggleHavok();

	void toggleVisual();

	void setVisualDisplayed(boolean newShowVisual);

	AvatarLocation getAvatarLocation();

	void setPhysicsEnabled(boolean enable);

	AvatarCollisionInfo getAvatarCollisionInfo();

	ViewingPlatform getViewingPlatform();

	void setAzerty(boolean a);

	void setMouseLock(boolean mouseLock);

	boolean isTrailorCam();

}