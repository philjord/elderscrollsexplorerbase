package scrollsexplorer.simpleclient.scenegraph;

import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupOnElapsedTime;

import scrollsexplorer.IDashboard;
import tools3d.mixed3d2d.curvehud.elements.HUDText;
import tools3d.utils.Utils3D;

public class LoadingInfoBehavior extends Behavior
{
	public static int TIME_SAMPLE = 500;
	private WakeupOnElapsedTime wakeUp = new WakeupOnElapsedTime(TIME_SAMPLE);

	private HUDText hudText;

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
		// we just crazily go and ask the explorer, esm, bsa (bsaset) for a message
		// explorer will return waiting if nothing happening or it'll describe the visual or physicsal load process somehow
		// esm and bsa will just describe indexing process

		//but for now just use the newtdashboard
		String loadingNow = "";
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
