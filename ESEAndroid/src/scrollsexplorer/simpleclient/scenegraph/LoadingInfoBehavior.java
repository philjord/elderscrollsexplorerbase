package scrollsexplorer.simpleclient.scenegraph;

import java.util.Enumeration;

import org.jogamp.java3d.Behavior;
import org.jogamp.java3d.WakeupOnElapsedTime;

import scrollsexplorer.IDashboard;
import tools3d.mixed3d2d.curvehud.elements.HUDText;
import tools3d.utils.Utils3D;

public class LoadingInfoBehavior extends Behavior
{
	public static int TIME_SAMPLE = 500;
	private WakeupOnElapsedTime wakeUp = new WakeupOnElapsedTime(TIME_SAMPLE);

	private HUDText hudText;

	private long startTime = Long.MIN_VALUE;

	public LoadingInfoBehavior(HUDText hudText)
	{
		this.hudText = hudText;
		setSchedulingBounds(Utils3D.defaultBounds);
	}

	public void initialize()
	{
		wakeupOn(wakeUp);
	}

	@SuppressWarnings({ "rawtypes" })
	public void processStimulus(Enumeration critera)
	{
		if (startTime == Long.MIN_VALUE)
			startTime = System.currentTimeMillis();

		int elapsedSeconds = (int) ((System.currentTimeMillis() - startTime) / 1000);

		// we just crazily go and ask the explorer, esm, bsa (bsaset) for a message
		// explorer will return waiting if nothing happening or it'll describe the visual or physicsal load process somehow
		// esm and bsa will just describe indexing process

		//but for now just use the newtdashboard
		String loadingNow = "" + elapsedSeconds + " : ";
		if (IDashboard.dashboard.getEsmLoading() > 0)
		{
			loadingNow += "ESM file loading ";
		}
		if (IDashboard.dashboard.getCellLoading() > 0)
		{
			loadingNow += "Cell loading ";
		}
		if (IDashboard.dashboard.getNearLoading() > 0)
		{
			loadingNow += "Near loading ";
		}
		if (IDashboard.dashboard.getFarLoading() > 0)
		{
			loadingNow += "Far loading ";
		}
		if (IDashboard.dashboard.getLodLoading() > 0)
		{
			loadingNow += "Lod loading ";
		}

		hudText.setText(loadingNow);

		wakeupOn(wakeUp);
	}
}
