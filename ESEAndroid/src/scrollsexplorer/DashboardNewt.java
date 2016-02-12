package scrollsexplorer;

import scrollsexplorer.simpleclient.physics.PhysicsSystem;

public class DashboardNewt extends IDashboard
{

	private int esmLoading = 0;

	private int cellLoading = 0;

	private int nearLoading = 0;

	private int farLoading = 0;

	private int lodLoading = 0;

	public DashboardNewt()
	{
		scrollsexplorer.IDashboard.dashboard = this;
	}

	public void setPhysicSystem(PhysicsSystem physicsSystem)
	{

	}

	public void setEsmLoading(int isLoading)
	{
		esmLoading += isLoading;

	}

	public void setCellLoading(int isLoading)
	{
		cellLoading += isLoading;

	}

	public void setNearLoading(int isLoading)
	{
		nearLoading += isLoading;

	}

	public void setFarLoading(int isLoading)
	{
		farLoading += isLoading;

	}

	public void setLodLoading(int isLoading)
	{
		lodLoading += isLoading;

	}
}
