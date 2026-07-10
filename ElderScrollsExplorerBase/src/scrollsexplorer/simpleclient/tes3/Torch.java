package scrollsexplorer.simpleclient.tes3;

import java.util.Iterator;

import org.jogamp.java3d.Behavior;
import org.jogamp.java3d.BoundingLeaf;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Light;
import org.jogamp.java3d.PointLight;
import org.jogamp.java3d.SpotLight;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.WakeupCriterion;
import org.jogamp.java3d.WakeupOnElapsedTime;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.J3dNiAVObject;
import tools3d.utils.Utils3D;
import utils.source.MediaSources;

public class Torch extends BranchGroup {
	private Light					light		= null;

	private BoundingLeaf			bl			= new BoundingLeaf();

	private LightFlickerBehavior	lightFlickerBehavior;
	TransformGroup shakeyHand = new TransformGroup();
	Transform3D handT = new Transform3D();

	private J3dNiAVObject			j3dNiAVObject;

	private boolean					lightOn		= true;
	private Color3f					color		= null;
	private Point3f					position	= null;
	private Point3f					attenuation	= null;

	private Vector3f				direction;
	private float					spreadAngle;
	private float					concentration;

	// -1 will use attenuation to work out
	private float					radius		= -1;
	// note radius and attenuation are strongly related, it is used only if there is no attenuation
	/* A PointLight is attenuated by the reciprocal of the sum of:
	 * <p>
	 * <ul>
	 * The constant attenuation factor<br>
	 * The Linear attenuation factor times the distance between the light
	 * and the vertex being illuminated<br>
	 * The quadratic attenuation factor times the square of the distance
	 * between the light and the vertex
	 * </ul>*/
	//so example 1, 1.5, 0 at 10 meters = 1/(1+15+0) = 1/16th 
	// so if we started at a strength of 0.5 (=color (r+g+b)/3 3=num of compoenents) e.g. 0.7+0.3+0.5 = 1.5/3 = 50%
	// so at 10 meters 1/32nd power
	// hence a radius of say 1/50th power max (bit arbitrary) = 0.02 = 1/(x+y*d+z*d*d) * (rgb/3), note  max r=d in this maths
	// https://softmath.com/algebra-help/algebra-calculator-rearranging.html#c=solve&v1=0.02%253D1%2F%2528x%2By*d%2Bz*d*d%2529*%2528%255Cleft%2528rgb%255Cright%2529%2F3%2529&v3=d
	// too complex to use
	// how about ignore d^2 quad factor as a bit mad
	// d = (rgb-0.06x)/0.06y nice

	public Torch(MediaSources mediaSources) {
		// use some reasonable defaults
		this(mediaSources, true, new Color3f(0.9f, 0.86f, 0.85f), new Point3f(0, 0.2f, 0), new Point3f(0, 1.1f, 0),
				null, 0f, 0f, -1f);
	}

	public Torch(	MediaSources mediaSources, boolean lightOn, Color3f color, Point3f position, Point3f attenuation,
					Vector3f direction, float spreadAngle, float concentration, float radius) {
		this.lightOn = lightOn;
		this.color = color;//new Color3f(0.9f, 0.86f, 0.85f) is a bit torchy
		this.position = position;// I had this but it's a bit weird new Point3f(0.5f, 0f, -0.25f);
		this.attenuation = attenuation;
		this.direction = direction;//new Vector3f(0, 0, -1) is a good idea
		this.spreadAngle = spreadAngle;
		this.concentration = concentration; // 0 is a good idea
		this.radius = radius;

		// radius overrides attenuation cos it's in the game file
		if (radius == -1) {
			if (attenuation.y > 1) {
				radius = ((color.x + color.y + color.z) - (0.06f * attenuation.x)) / (0.06f * attenuation.y);
			} else {
				radius = 100f;
			}
		}

	/*	if (false) {
			String nifFileName = "blah";

			NifJ3dVisRoot vr = NifToJ3d.loadShapes(nifFileName, mediaSources.getMeshSource(),
					mediaSources.getTextureSource());
			j3dNiAVObject = vr.getVisualRoot();

			// let's find out where the attach point is
			Vector3f attachNode = findAttachLight(j3dNiAVObject.getNiAVObject(), vr.getNiToJ3dData());
			//if (attachNode != null)
			//	position = new Point3f(attachNode);

			if (j3dNiAVObject != null) {
				addChild(j3dNiAVObject);
			}

		}*/

		//System.out.println("new light " + color);
		//System.out.println("falls fade " + ligh.fade + " falloffExponent " + ligh.falloffExponent + " fieldOfView " + ligh.fieldOfView);
		//System.out.println("ligh.radius " + ligh.radius + " " + (ligh.radius * ESConfig.ES_TO_METERS_SCALE));
		if (spreadAngle <= 0 || spreadAngle >= 90f) {
			light = new PointLight(true, color, position, attenuation);
		} else {
			light = new SpotLight(true, color, position, attenuation, direction, spreadAngle, concentration);
		}
		light.setCapability(Light.ALLOW_STATE_WRITE);
		light.setCapability(Light.ALLOW_COLOR_WRITE);
		bl.setRegion(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), radius));
		light.setInfluencingBoundingLeaf(bl);
		addChild(bl);
		
		
		shakeyHand.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		shakeyHand.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		//TODO: modify this a bit more
		//handT.setTranslation(position);
		
		shakeyHand.addChild(light);
		addChild(shakeyHand);


		//add the flickering effect in with a behaviour (just up and down intensity of each color randomly a bit)
		lightFlickerBehavior = new LightFlickerBehavior(light);
		lightFlickerBehavior.setEnable(true);
		lightFlickerBehavior.setSchedulingBounds(Utils3D.defaultBounds);
		addChild(lightFlickerBehavior);

		// turn it off by default
		toggle();
	}

/*	public Vector3f findAttachLight(NiAVObject niAVObject, NiToJ3dData niToJ3dData) {
		//TODO: this should be the classic multiply up the chain gear
		if (niAVObject.name.equals("AttachLight")) {
			return ConvertFromNif.toJ3d(niAVObject.translation);
		}

		if (niAVObject instanceof NiNode) {
			NiNode niNode = (NiNode)niAVObject;
			for (int i = 0; i < niNode.numChildren; i++) {
				NiObject o = niToJ3dData.get(niNode.children[i]);
				if (o != null && o instanceof NiNode) {
					NiNode childNode = (NiNode)o;
					Vector3f v = findAttachLight(childNode, niToJ3dData);
					if (v != null)
						return v;

				}
			}
		}

		return null;
	}*/

	public void toggle() {
		lightOn = !lightOn;
		light.setEnable(lightOn);
	}

	//TODO flicker should also move the flame a bit so it's make normals shimmer
	// note a hand held torch would just be movey movey
	private class LightFlickerBehavior extends Behavior {
		private Light				lightToFlicker;
		private Color3f				originalColor	= new Color3f();
		private Color3f				updateColor		= new Color3f();

		private WakeupOnElapsedTime	wakeUp;

		public LightFlickerBehavior(Light lightToFlicker) {
			this.lightToFlicker = lightToFlicker;
			lightToFlicker.getColor(originalColor);
			wakeUp = new WakeupOnElapsedTime(50);
		}

		@Override
		public void initialize() {
			wakeupOn(wakeUp);
		}

		@Override
		public void processStimulus(Iterator<WakeupCriterion> critiria) {
			float strength = (float)((Math.random() * 0.1) - 0.05);
			float dr = (float)((Math.random() * 0.05) - 0.025);
			float dg = (float)((Math.random() * 0.05) - 0.025);
			float db = (float)((Math.random() * 0.05) - 0.025);
			updateColor.x = originalColor.x * (1f + strength + dr);
			updateColor.y = originalColor.y * (1f + strength + dg);
			updateColor.z = originalColor.z * (1f + strength + db);
			lightToFlicker.setColor(updateColor);

			//reset the wakeup
			wakeupOn(wakeUp);
		}
	}
}
