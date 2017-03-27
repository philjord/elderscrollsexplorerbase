package scrollsexplorer.simpleclient.tes3;

import java.util.Iterator;

import org.jogamp.java3d.Behavior;
import org.jogamp.java3d.BoundingLeaf;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Light;
import org.jogamp.java3d.PointLight;
import org.jogamp.java3d.SpotLight;
import org.jogamp.java3d.WakeupCriterion;
import org.jogamp.java3d.WakeupOnElapsedTime;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import esmj3d.j3d.BethRenderSettings;
import nif.NifJ3dVisRoot;
import nif.NifToJ3d;
import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import tools3d.utils.Utils3D;
import utils.ESConfig;
import utils.source.MediaSources;

public class Torch extends BranchGroup
{
	private Light light = null;

	private BoundingLeaf bl = new BoundingLeaf();

	private LightFlickerBehavior lightFlickerBehavior;

	private J3dNiAVObject j3dNiAVObject;

	private boolean isOn = true;

	private float fieldOfView = -1;
	private float fade = 0;
	private float falloffExponent = 0;
	private float radius = 30f;

	public Torch(MediaSources mediaSources)
	{

		Point3f lightPosition = new Point3f(0.5f, 0f, -0.25f);
		if (false)
		{
			String nifFileName = "blah";

			NifJ3dVisRoot vr = NifToJ3d.loadShapes(nifFileName, mediaSources.getMeshSource(), mediaSources.getTextureSource());
			j3dNiAVObject = vr.getVisualRoot();

			// let's find out where the attach point is
			Vector3f attachNode = findAttachLight(j3dNiAVObject.getNiAVObject(), vr.getNiToJ3dData());
			if (attachNode != null)
				lightPosition = new Point3f(attachNode);

			if (j3dNiAVObject != null)
			{
				addChild(j3dNiAVObject);
			}

		}

		Color3f color = new Color3f(0.9f, 0.85f, 0.86f);
		//System.out.println("new light " + color);
		//System.out.println("falls fade " + ligh.fade + " falloffExponent " + ligh.falloffExponent + " fieldOfView " + ligh.fieldOfView);
		//System.out.println("ligh.radius " + ligh.radius + " " + (ligh.radius * ESConfig.ES_TO_METERS_SCALE));
		if (fieldOfView == -1 || fieldOfView >= 90f)
		{
			light = new PointLight(true, color, lightPosition, new Point3f(1, fade, falloffExponent));
		}
		else
		{
			light = new SpotLight(true, color, lightPosition, new Point3f(1, fade, falloffExponent), new Vector3f(0, 0, -1), fieldOfView,
					0);
		}
		light.setCapability(Light.ALLOW_STATE_WRITE);
		light.setCapability(Light.ALLOW_COLOR_WRITE);
		bl.setRegion(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), radius));
		light.setInfluencingBoundingLeaf(bl);
		addChild(bl);
		addChild(light);

		// for debug visualizing the radius and color (badly)
		//Cube c =  new Cube(ligh.radius * ESConfig.ES_TO_METERS_SCALE );
		//c.setAppearance(new SimpleShaderAppearance(color));			
		//addChild(c);

		//TODO: add the flickering effect in with a behaviour (just up and down intensity of each color randomly a bit)
		lightFlickerBehavior = new LightFlickerBehavior(light);
		lightFlickerBehavior.setEnable(true);
		lightFlickerBehavior.setSchedulingBounds(Utils3D.defaultBounds);
		addChild(lightFlickerBehavior);

		// turn it off by default
		toggle();
	}

	public Vector3f findAttachLight(NiAVObject niAVObject, NiToJ3dData niToJ3dData)
	{
		//TODO: this should be the classic multiply up the chain gear
		if (niAVObject.name.equals("AttachLight"))
		{
			return new Vector3f(niAVObject.translation.x * ESConfig.ES_TO_METERS_SCALE,
					niAVObject.translation.z * ESConfig.ES_TO_METERS_SCALE, -niAVObject.translation.y * ESConfig.ES_TO_METERS_SCALE);
		}

		if (niAVObject instanceof NiNode)
		{
			NiNode niNode = (NiNode) niAVObject;
			for (int i = 0; i < niNode.numChildren; i++)
			{
				NiObject o = niToJ3dData.get(niNode.children[i]);
				if (o != null && o instanceof NiNode)
				{
					NiNode childNode = (NiNode) o;
					Vector3f v = findAttachLight(childNode, niToJ3dData);
					if (v != null)
						return v;

				}
			}
		}

		return null;
	}

	public void toggle()
	{
		isOn = !isOn;
		light.setEnable(isOn);
	}

	private class LightFlickerBehavior extends Behavior
	{
		private Light lightToFlicker;
		private Color3f originalColor = new Color3f();
		private Color3f updateColor = new Color3f();

		private WakeupOnElapsedTime wakeUp;

		public LightFlickerBehavior(Light lightToFlicker)
		{
			this.lightToFlicker = lightToFlicker;
			lightToFlicker.getColor(originalColor);
			wakeUp = new WakeupOnElapsedTime(50);
		}

		@Override
		public void initialize()
		{
			wakeupOn(wakeUp);
		}

		@Override
		public void processStimulus(Iterator<WakeupCriterion> critiria)
		{
			float dr = (float) ((Math.random() * 0.2) - 0.1);
			float dg = (float) ((Math.random() * 0.2) - 0.1);
			float db = (float) ((Math.random() * 0.2) - 0.1);
			updateColor.x = originalColor.x * (1f + dr);
			updateColor.y = originalColor.y * (1f + dg);
			updateColor.z = originalColor.z * (1f + db);
			lightToFlicker.setColor(updateColor);

			//reset the wakeup
			wakeupOn(wakeUp);
		}
	}
}
