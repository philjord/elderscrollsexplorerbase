package scrollsexplorer;

import scrollsexplorer.simpleclient.physics.PhysicsSystem;

//FIXME: what a rubbish system we have here
public abstract class IDashboard
{
	public static IDashboard dashboard;

	public abstract void setPhysicSystem(PhysicsSystem physicsSystem);

	public abstract void setEsmLoading(int isLoading);

	public abstract void setCellLoading(int isLoading);

	public abstract void setNearLoading(int isLoading);

	public abstract void setFarLoading(int isLoading);

	public abstract void setLodLoading(int isLoading);

	public abstract int getEsmLoading();

	public abstract int getCellLoading();

	public abstract int getNearLoading();

	public abstract int getFarLoading();

	public abstract int getLodLoading();
}
