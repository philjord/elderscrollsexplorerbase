package nif.shaders;

import java.io.IOException;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shader;
import javax.media.j3d.ShaderAppearance;
import javax.media.j3d.ShaderAttributeSet;
import javax.media.j3d.ShaderAttributeValue;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TextureCubeMap;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import nif.BgsmSource;
import nif.NifVer;
import nif.compound.NifColor3;
import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifTexCoord;
import nif.enums.BSLightingShaderPropertyShaderType;
import nif.enums.FaceDrawMode;
import nif.enums.SkyrimShaderPropertyFlags1;
import nif.enums.SkyrimShaderPropertyFlags2;
import nif.enums.TexClampMode;
import nif.j3d.J3dNiGeometry;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiGeometry;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiSpecularProperty;
import nif.niobject.NiStencilProperty;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiWireframeProperty;
import nif.niobject.NiZBufferProperty;
import nif.niobject.bgsm.BgsmFile;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSShaderLightingProperty;
import nif.niobject.bs.BSShaderNoLightingProperty;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSShaderProperty;
import nif.niobject.bs.BSShaderTextureSet;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.shaders.ShaderPrograms.Program;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

/**
 this will build an appearance up out of a NiGeometry that can be used by a real j3dnigeometry 
 It is based on(copied from?)the nifskope 2.0 renderer code from jonwd7
 
 
 TODO:S 
 Vertex attribute arrays for tan and bi, so tex coords can go back to 2 dim
 tonemap function in shaders code, make one up
 bool in shader code would be nicer as bool, perhaps push forward on GL number to 4?? Java3D can't send them through as
 GL2 native calls don't accept them
 The SKYRIM TREE ANIM code in the bind, is useless but should be put into a new shader type
 
 sk_env must have the wrld matrix sent through, perhaps just a dummy to ensure the variable is bound?
 
 https://gist.github.com/patriciogonzalezvivo/3a81453a24a542aabc63 looks like some real good lighting equations
*/

public class NiGeometryAppearanceShader
{
	//private static WeakHashMap<NiProperty, NodeComponent> propertyLookup = new WeakHashMap<NiProperty, NodeComponent>();

	//private static WeakHashMap<BSLightingShaderProperty, NodeComponent> bsLightingShaderPropertyLookup = new WeakHashMap<BSLightingShaderProperty, NodeComponent>();

	private TextureSource textureSource;

	private NiToJ3dData niToJ3dData;

	private ShaderAppearance app = new ShaderAppearance();
	private Material mat = new Material();
	private RenderingAttributes ra = new RenderingAttributes();
	private PolygonAttributes pa = new PolygonAttributes();
	private TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.NONE, 0f);

	private GLSLShaderProgram2 shaderProgram = null;

	private ShaderAttributeSet shaderAttributeSet = null;

	private ArrayList<ShaderAttributeValue2> allShaderAttributeValues = new ArrayList<ShaderAttributeValue2>();
	private ArrayList<TextureUnitState> allTextureUnitStates = new ArrayList<TextureUnitState>();

	private Program selectedProgram;

	private int texunit = 0;

	public NiGeometryAppearanceShader(TextureSource textureSource, NiToJ3dData niToJ3dData)
	{
		this.textureSource = textureSource;
		this.niToJ3dData = niToJ3dData;

		//ensure tangetns loaded to geometries
		J3dNiTriBasedGeom.TANGENTS_BITANGENTS = true;
		J3dNiTriBasedGeom.INTERLEAVE = false;

		//configure app defaults	
		mat.setLightingEnable(true);
		mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);

		app.setMaterial(mat);

		//MUST always be set on everything (stencils!)
		app.setRenderingAttributes(ra);

		app.setPolygonAttributes(pa);

		app.setTransparencyAttributes(ta);

	}

	public Appearance getAppearance()
	{
		return app;
	}

	public String setupShaderProgram(NiGeometry niGeometry, NiToJ3dData niToJ3dData, String hint)
	{
		ShaderPrograms.loadShaderPrograms();

		PropertyList props = new PropertyList(niGeometry.properties, niToJ3dData);

		if (hint != null)
		{
			ShaderPrograms.Program program = ShaderPrograms.programs.get(hint);

			if (program != null && program.isStatusOk() && setupProgram(program, niGeometry, niToJ3dData, props))
				return hint;
		}

		for (ShaderPrograms.Program program : ShaderPrograms.programs.values())
		{
			//System.out.println("program checked " + program);
			if (program.isStatusOk() && setupProgram(program, niGeometry, niToJ3dData, props))
				return program.getName();
		}

		//null mean use fixed
		return null;
	}

	private boolean setupProgram(ShaderPrograms.Program prog, NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props)
	{

		if (!prog.conditions.eval(niGeometry, niToJ3dData, props))
			return false;

		this.selectedProgram = prog;

		// texturing

		NiTexturingProperty texprop = (NiTexturingProperty) props.get(NiTexturingProperty.class);
		BSShaderLightingProperty bsprop = (BSShaderLightingProperty) props.get(BSShaderLightingProperty.class);
		BSLightingShaderProperty bslsp = props.getBSLightingShaderProperty();

		if (texprop == null && bsprop == null && bslsp == null)
			return false;

		int clamp = TexClampMode.WRAP_S_WRAP_T;

		if (bslsp != null)
		{
			clamp = bslsp.TextureClampMode.mode;
		}

		String textureUnitName = "BaseMap";
		if (texprop != null)
		{
			bind(textureUnitName, texprop, fileName(texprop, 0), clamp);
		}
		else if (bsprop != null)
		{
			bind(textureUnitName, bsprop, fileName(bsprop, 0), clamp);
		}
		else if (bslsp != null)
		{
			bind(textureUnitName, bslsp, fileName(bslsp, 0), clamp);
		}

		textureUnitName = "NormalMap";
		if (texprop != null)
		{
			String fname = fileName(texprop, 0);

			if (fname.isEmpty())
				return false;

			int pos = fname.indexOf("_");

			if (pos >= 0)
				fname = fname.substring(0, pos) + "_n.dds";
			else if ((pos = fname.lastIndexOf(".")) >= 0)
				fname = fname.substring(0, pos) + "_n" + fname.substring(pos);

			bind(textureUnitName, texprop, fname, clamp);
		}
		else if (bsprop != null)
		{
			bind(textureUnitName, bsprop, fileName(bsprop, 1), clamp);
		}
		else if (bslsp != null)
		{
			bind(textureUnitName, bslsp, fileName(bslsp, 1), clamp);
		}

		textureUnitName = "GlowMap";
		if (texprop != null)
		{
			String fname = fileName(texprop, 0);

			if (fname.isEmpty())
				return false;

			int pos = fname.indexOf("_");

			if (pos >= 0)
				fname = fname.substring(0, pos) + "_g.dds";
			else if ((pos = fname.lastIndexOf(".")) >= 0)
				fname = fname.substring(0, pos) + "_g" + fname.substring(pos);

			bind(textureUnitName, texprop, fname, clamp);
		}
		else if (bsprop != null)
		{
			bind(textureUnitName, bsprop, fileName(bsprop, 2), clamp);
		}
		else if (bslsp != null)
		{
			bind(textureUnitName, bslsp, fileName(bslsp, 2), clamp);
		}

		String white = "shaders/nif/white.dds";
		//String black = "shaders/nif/black.dds";
		String default_n = "shaders/nif/default_n.dds";

		// BSLightingShaderProperty
		if (bslsp != null)
		{
			uni1f("lightingEffect1", bslsp.LightingEffect1);
			uni1f("lightingEffect2", bslsp.LightingEffect2);

			uni1f("alpha", bslsp.Alpha);

			uni2f("uvScale", bslsp.UVScale.u, bslsp.UVScale.v);

			uni2f("uvOffset", bslsp.UVOffSet.u, bslsp.UVOffSet.v);

			//TODO: what the hell do I do here?? modify the shaders no doubt!
			/*uni4m( "viewMatrix", mesh.viewTrans().toMatrix4() );
			uni4m( "viewMatrixInverse", mesh.viewTrans().toMatrix4().inverted() );
			
			uni4m( "localMatrix", mesh.localTrans().toMatrix4() );
			uni4m( "localMatrixInverse", mesh.localTrans().toMatrix4().inverted() );
			
			uni4m( "worldMatrix", mesh.worldTrans().toMatrix4() );
			uni4m( "worldMatrixInverse", mesh.worldTrans().toMatrix4().inverted() );*/

			//sk_env.frag sk_multilayer.frag uses the worldMatrix  
			//sk_msn.frag uses  viewMatrix (msn stand for model space normal mapping)

			boolean hasTintColor = bslsp.HairTintColor != null || bslsp.SkinTintColor != null;
			uni1i("hasTintColor", hasTintColor);
			if (hasTintColor)
			{
				NifColor3 tC = bslsp.HairTintColor != null ? bslsp.HairTintColor : bslsp.SkinTintColor;
				uni3f("tintColor", tC.r, tC.g, tC.b);
			}

			boolean hasTintMask = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_FaceTint;
			boolean hasDetailMask = hasTintMask;

			uni1i("hasDetailMask", hasDetailMask);
			if (hasDetailMask)
			{
				bind("DetailMask", bslsp, fileName(bslsp, 3, "shaders/nif/blankdetailmap.dds"), clamp);
			}

			uni1i("hasDetailMask", hasTintMask);
			if (hasTintMask)
			{
				bind("TintMask", bslsp, fileName(bslsp, 6, "shaders/nif/gray.dds"), clamp);
			}

			// Rim & Soft params
			boolean hasSoftlight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Soft_Lighting);
			boolean hasRimlight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Rim_Lighting);

			uni1i("hasSoftlight", hasSoftlight);
			uni1i("hasRimlight", hasRimlight);

			if (hasSoftlight || hasRimlight)
			{
				bind("LightMask", bslsp, fileName(bslsp, 2, default_n), clamp);
			}

			// Backlight params
			boolean hasBacklight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Back_Lighting);
			uni1i("hasBacklight", hasBacklight);

			if (hasBacklight)
			{
				bind("BacklightMap", bslsp, fileName(bslsp, 7, default_n), clamp);
			}

			// Glow params

			boolean hasEmittance = bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Own_Emit);
			uni1i("hasEmit", hasEmittance);
			if (hasEmittance)
				uni1f("glowMult", bslsp.EmissiveMultiple);
			else
				uni1f("glowMult", 0);

			boolean hasGlowMap = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Glow_Map) && hasFileName(bslsp, 2);
			uni1i("hasGlowMap", hasGlowMap);

			uni3f("glowColor", bslsp.EmissiveColor.r, bslsp.EmissiveColor.g, bslsp.EmissiveColor.g);

			// Specular params

			uni1f("specStrength", bslsp.SpecularStrength);

			// Assure specular power does not break the shaders
			float gloss = bslsp.Glossiness;
			uni1f("specGlossiness", (gloss > 0.0) ? gloss : 1.0f);

			uni3f("specColor", bslsp.SpecularColor.r, bslsp.SpecularColor.g, bslsp.SpecularColor.b);

			boolean hasSpecularMap = bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Specular) && hasFileName(bslsp, 7);
			uni1i("hasSpecularMap", hasSpecularMap);

			if (hasSpecularMap && !hasBacklight)
			{
				bind("SpecularMap", bslsp, fileName(bslsp, 7, default_n), clamp);
			}

			// Multi-Layer
			boolean hasMultiLayerParallax = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Multi_Layer_Parallax);
			if (hasMultiLayerParallax)
			{
				NifTexCoord inS = bslsp.ParallaxInnerLayerTextureScale;
				uni2f("innerScale", inS.u, inS.v);

				uni1f("innerThickness", bslsp.ParallaxInnerLayerThickness);

				uni1f("outerRefraction", bslsp.ParallaxRefractionScale);
				uni1f("outerReflection", bslsp.ParallaxEnvmapStrength);

				bind("InnerMap", bslsp, fileName(bslsp, 6, default_n), clamp);
			}

			// Environment Mapping
			boolean hasEnvironmentMap = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap
					&& bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Environment_Mapping);

			hasEnvironmentMap |= bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap
					&& bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Eye_Environment_Mapping);
			hasEnvironmentMap |= bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Multi_Layer_Parallax);

			boolean hasCubeMap = (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap
					|| bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap
					|| bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_MultiLayerParallax) && hasEnvironmentMap
					&& hasFileName(bslsp, 4);

			uni1i("hasCubeMap", hasCubeMap);

			boolean useEnvironmentMask = hasEnvironmentMap && hasFileName(bslsp, 5);

			uni1i("hasEnvMask", useEnvironmentMask);

			if (hasCubeMap && hasEnvironmentMap)
			{
				float envReflection = 0;
				if (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap)
					envReflection = bslsp.EnvironmentMapScale;
				else if (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap)
					envReflection = bslsp.EyeCubemapScale;

				uni1f("envReflection", envReflection);

				bind("EnvironmentMap", bslsp, fileName(bslsp, 5, white), clamp);

				bindCube("CubeMap", bslsp, fileName(bslsp, 4));

			}
			else
			{
				// In the case that the cube texture has already been bound,
				//	but SLSF1_Environment_Mapping is not set, assure that it 
				//	removes reflections.
				uni1f("envReflection", 0);
			}

			// Parallax
			boolean hasHeightMap = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_Heightmap;
			hasHeightMap |= bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Parallax) && hasFileName(bslsp, 3);

			if (hasHeightMap)
			{
				bind("HeightMap", bslsp, fileName(bslsp, 3, "shaders/nif/gray.dds"), clamp);
			}
			
			
			
			//PJ new gear			
			//TODO: check this jonwd7 does not do it
			// apparently the The vertex colors are used as well, just not the alpha component when
			// SF_Vertex_Animation is present
			// http://niftools.sourceforge.net/forum/viewtopic.php?f=10&t=3276
			if (bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim))
			{
				//hand through the isTreeAnim flag so the shader can ignore alpha
				//textureAttributes.setTextureMode(TextureAttributes.COMBINE);
				//textureAttributes.setCombineAlphaMode(TextureAttributes.COMBINE_REPLACE);

				pa.setCullFace(PolygonAttributes.CULL_NONE);
				pa.setBackFaceNormalFlip(true);
			}
			
		}

		BSEffectShaderProperty bsesp = (BSEffectShaderProperty) props.get(BSEffectShaderProperty.class);
		if (niGeometry.nVer.LOAD_USER_VER2 < 130 && bsesp != null)
		{
			clamp = bsesp.TextureClampMode.mode;
			clamp = clamp ^ TexClampMode.MIRRORED_S_MIRRORED_T;

			bind("SourceTexture", bsesp, bsesp.SourceTexture, clamp);

			boolean isDoubleSided = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Double_Sided);
			uni1i("doubleSided", isDoubleSided);

			uni2f("uvScale", bsesp.UVScale.u, bsesp.UVScale.v);

			uni2f("uvOffset", bsesp.UVOffSet.u, bsesp.UVOffSet.v);

			boolean hasSourceTexture = bsesp.SourceTexture != null && bsesp.SourceTexture.trim().length() > 0;
			uni1i("hasSourceTexture", hasSourceTexture);
			boolean hasGreyscaleMap = bsesp.GreyscaleTexture != null && bsesp.GreyscaleTexture.trim().length() > 0;
			uni1i("hasGreyscaleMap", hasGreyscaleMap);

			boolean greyscaleAlpha = bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteAlpha);
			boolean greyscaleColor = bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Greyscale_To_PaletteColor);
			uni1i("greyscaleAlpha", greyscaleAlpha);
			uni1i("greyscaleColor", greyscaleColor);

			boolean useFalloff = bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Use_Falloff);
			boolean vertexAlpha = bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Vertex_Alpha);
			boolean vertexColors = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Vertex_Colors);
			uni1i("useFalloff", useFalloff);
			uni1i("vertexAlpha", vertexAlpha);
			uni1i("vertexColors", vertexColors);

			boolean hasWeaponBlood = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Weapon_Blood);
			uni1i("hasWeaponBlood", hasWeaponBlood);

			// Glow params

			uni4f("glowColor", bsesp.EmissiveColor.r, bsesp.EmissiveColor.g, bsesp.EmissiveColor.b, bsesp.EmissiveColor.a);
			uni1f("glowMult", bsesp.EmissiveMultiple);

			// Falloff params

			uni4f("falloffParams", bsesp.FalloffStartAngle, bsesp.FalloffStopAngle, bsesp.FalloffStartOpacity, bsesp.FalloffStopOpacity);

			uni1f("falloffDepth", bsesp.SoftFalloffDepth);

			// BSEffectShader textures
			bind("GreyscaleMap", bsesp, bsesp.GreyscaleTexture, TexClampMode.MIRRORED_S_MIRRORED_T);

		}

		// Defaults for uniforms in older meshes
		if (bsesp == null && bslsp == null)
		{
			uni2f("uvScale", 1.0f, 1.0f);
			uni2f("uvOffset", 0.0f, 0.0f);
		}

		System.out.println("using prog " + prog.getName());
		//for (String name : prog.shaders.keySet())
		//	System.out.println("shaderCode " + name + "\n" + prog.shaders.get(name).getShaderSource());

		shaderProgram = new GLSLShaderProgram2();
		shaderProgram.name = prog.getName();
		shaderProgram.setShaders(prog.shaders.values().toArray(new Shader[] {}));
		shaderAttributeSet = new ShaderAttributeSet();

		if (programHasVar("tangent", 0))
		{
			shaderProgram.setVertexAttrNames(new String[] { "tangent", "binormal" });
			System.out.println("set attribute names");
		}

		String[] shaderAttrNames = new String[allShaderAttributeValues.size()];
		int i = 0;
		for (ShaderAttributeValue sav : allShaderAttributeValues)
		{
			System.out.println("i= " + i + " " + sav.getAttributeName() + " " + sav.getValue());
			shaderAttrNames[i++] = sav.getAttributeName();
			shaderAttributeSet.put(sav);
		}

		shaderProgram.setShaderAttrNames(shaderAttrNames);

		TextureUnitState[] tus = allTextureUnitStates.toArray(new TextureUnitState[] {});
		for (TextureUnitState tu : tus)
		{
			System.out.println("tu " + tu.getName());
		}
		app.setTextureUnitState(tus);
		app.setShaderProgram(shaderProgram);
		app.setShaderAttributeSet(shaderAttributeSet);

		// setup blending

		glProperty((NiAlphaProperty) props.get(NiAlphaProperty.class));

		// BSESP/BSLSP do not always need an NiAlphaProperty, and appear to override it at times

		boolean translucent = (bslsp != null)
				&& (bslsp.Alpha < 1.0f || bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Refraction));

		translucent |= (bsesp != null) && props.get(NiAlphaProperty.class) == null && bsesp.EmissiveColor.a < 1.0f;

		if (translucent)
		{
			//Note my old code also used1-bslsp.alpha here
			ta.setTransparency(0.1f);
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
		}

		// setup material

		glProperty((NiMaterialProperty) props.get(NiMaterialProperty.class), (NiSpecularProperty) props.get(NiSpecularProperty.class));

		// setup z buffer

		glProperty((NiZBufferProperty) props.get(NiZBufferProperty.class));

		boolean depthTest = true;
		depthTest |= (bslsp != null) && bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_ZBuffer_Test);
		depthTest |= (bsesp != null) && bsesp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_ZBuffer_Test);

		if (!depthTest)
		{
			ra.setDepthBufferEnable(false);
		}

		boolean depthWrite = true;
		depthWrite |= (bslsp != null) && bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_ZBuffer_Write);
		depthWrite |= (bsesp != null) && bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_ZBuffer_Write);
		if (!depthWrite || translucent)
		{
			ra.setDepthBufferWriteEnable(false);
		}

		// setup stencil

		glProperty((NiStencilProperty) props.get(NiStencilProperty.class));

		// wireframe ?

		glProperty((NiWireframeProperty) props.get(NiWireframeProperty.class));

		return true;
	}

	private void glProperty(NiWireframeProperty nwp)
	{
		// TODO later
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
	}

	private void glProperty(NiStencilProperty nsp)
	{
		if (nsp != null)
		{
			if (nsp.getDrawMode() == FaceDrawMode.DRAW_BOTH)
			{
				pa.setCullFace(PolygonAttributes.CULL_NONE);
				pa.setBackFaceNormalFlip(true);
			}

			//TODO: jonwd7 does not do this?
			if (nsp.isStencilEnable())
			{
				ra.setStencilEnable(true);
				ra.setStencilWriteMask(nsp.stencilMask);
				ra.setStencilFunction(NifOpenGLToJava3D.convertStencilFunction(nsp.stencilFunction()), nsp.stencilRef, nsp.stencilMask);
				ra.setStencilOp(NifOpenGLToJava3D.convertStencilAction(nsp.failAction()), //
						NifOpenGLToJava3D.convertStencilAction(nsp.zFailAction()), //
						NifOpenGLToJava3D.convertStencilAction(nsp.passAction()));
			}
		}

	}

	private void glProperty(NiZBufferProperty nzp)
	{
		if (nzp != null)
		{
			//TODO: bit tricky
			/*	if (nzp.depthTest)
				{
					glEnable(GL_DEPTH_TEST);
					glDepthFunc(nzp. depthFunc);
				}
				else
				{
					glDisable(GL_DEPTH_TEST);
				}
			
				glDepthMask(nzp.depthMask ? GL_TRUE : GL_FALSE);*/
		}
	}

	private void glProperty(NiMaterialProperty nmp, NiSpecularProperty nsp)
	{
		if (nmp != null)
		{
			if (!(nmp.nVer.LOAD_VER == NifVer.VER_20_2_0_7 && (nmp.nVer.LOAD_USER_VER == 11 || nmp.nVer.LOAD_USER_VER == 12)
					&& nmp.nVer.LOAD_USER_VER2 > 21))
			{
				mat.setAmbientColor(nmp.ambientColor.r, nmp.ambientColor.g, nmp.ambientColor.b);
				mat.setDiffuseColor(nmp.diffuseColor.r, nmp.diffuseColor.g, nmp.diffuseColor.b);
			}

			mat.setEmissiveColor(nmp.emissiveColor.r, nmp.emissiveColor.g, nmp.emissiveColor.b);

			//jonwd7 says this is part of above, but I can't
			if (nmp.alpha != 1.0f)
			{
				ta.setTransparency(1 - nmp.alpha);
				ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			}

			if (nsp != null && (nsp.flags.flags & 0x01) == 0)
			{
				mat.setShininess(0.0f);
				mat.setSpecularColor(0, 0, 0);
			}
			else
			{
				mat.setShininess(nmp.glossiness);
				mat.setSpecularColor(nmp.specularColor.r, nmp.specularColor.g, nmp.specularColor.b);
			}
		}
		else
		{
			mat.setAmbientColor(new Color3f(0.4f, 0.4f, 0.4f));
			mat.setDiffuseColor(new Color3f(0.8f, 0.8f, 0.8f));
			mat.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));

			mat.setShininess(33f);//33 cos jonwd7 says it's a good default
		}

	}

	private void glProperty(NiAlphaProperty nap)
	{
		if (nap != null && nap.alphaBlendingEnable())
		{
			ta.setTransparencyMode(TransparencyAttributes.BLENDED);
			ta.setSrcBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.sourceBlendMode(), true));
			ta.setDstBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.destinationBlendMode(), false));
		}
		else
		{
			ta.setTransparencyMode(TransparencyAttributes.SCREEN_DOOR);
		}

		if (nap != null && nap.alphaTestEnabled())
		{
			// I think the PolygonAttributes.CULL_NONE should be applied to anything
			// with an alphaTestEnabled(), flat_lod trees from skyrim prove it
			// obviously transparent stuff can be seen from the back quite often
			// F:\game media\Fallout3\meshes\creatures\alien\alien.nif suggests not these?
			//pa.setCullFace(PolygonAttributes.CULL_NONE);
			//pa.setBackFaceNormalFlip(true);

			int alphaTestMode = NifOpenGLToJava3D.convertAlphaTestMode(nap.alphaTestMode());
			//Test of greater with threshold of 0 is in fact no alpha, and due to sorting issue better to turn off
			//TODO: test this correct for F:\game media\Fallout3\meshes\creatures\alien\alien.nif
			if (alphaTestMode == RenderingAttributes.GREATER && nap.threshold == 0)
			{
				ta.setTransparencyMode(TransparencyAttributes.NONE);
			}
			else
			{
				ra.setAlphaTestFunction(alphaTestMode);

				float threshold = ((nap.threshold) / 255f);// threshold range of 255 to 0 confirmed empirically
				ra.setAlphaTestValue(threshold);
			}
		}
		// do NOT disable no alpha test still enables transparent textures
		

	}

	// Sets a float
	private void uni1f(String var, float x)
	{
		if (programHasVar(var, x))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Float(x)));
	}

	// Sets a vec2 (two floats)
	private void uni2f(String var, float x, float y)
	{
		if (programHasVar(var, x, 2))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector2f(x, y)));
	}

	// Sets a vec3 (three floats)
	private void uni3f(String var, float x, float y, float z)
	{
		if (programHasVar(var, x, 3))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector3f(x, y, z)));
	}

	// Sets a vec4 (four floats)
	private void uni4f(String var, float x, float y, float z, float w)
	{
		if (programHasVar(var, x, 4))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Vector4f(x, y, z, w)));
	};

	// Sets a boolean
	private void uni1i(String var, boolean val)
	{
		if (programHasVar(var, 1))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Integer(val ? 1 : 0)));
	};

	// Sets an integer  
	private void uni1i(String var, int val)
	{
		if (programHasVar(var, val))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Integer(val)));
	};

	// Sets a mat3 (3x3 matrix)
	private void uni3m(String var, NifMatrix33 val)
	{
		if (programHasVar(var, 1.0f, 3))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Matrix3f(val.data())));
	}

	// Sets a mat4 (4x4 matrix)
	private void uni4m(String var, NifMatrix44 val)
	{
		if (programHasVar(var, 1.0f, 4))
			allShaderAttributeValues.add(new ShaderAttributeValue2(var, new Matrix4f(val.data())));
	};

	/**
	 * Apparently shader attributes can only be set if the shader code declares them otherwise 
	 * a type mismatch error comes back, who knew. Also problem happen if TUS get set with a name
	 * so must check before, also shaders will compile away variables totally
	 * @param var
	 * @param val
	 * @return
	 */
	private boolean programHasVar(String var)
	{
		for (SourceCodeShader2 s : selectedProgram.shaders.values())
		{
			if (s.shaderHasVar(var))
				return true;
		}
		return false;
	}

	private boolean programHasVar(String var, Object val)
	{
		for (SourceCodeShader2 s : selectedProgram.shaders.values())
		{
			if (s.shaderHasVar(var, val.getClass().getSimpleName()))
				return true;
		}
		return false;
	}

	private boolean programHasVar(String var, Object val, int arrSize)
	{
		for (SourceCodeShader2 s : selectedProgram.shaders.values())
		{
			if (s.shaderHasVar(var, val.getClass().getSimpleName(), arrSize))
				return true;
		}
		return false;
	}

	private void bindCube(String textureUnitName, BSLightingShaderProperty bslsp, String fileName)
	{
		//TODO: note nifskope bsa extractor has a cube property on dds files
		if (programHasVar(textureUnitName))
		{
			TextureUnitState tus = new TextureUnitState();
			//TextureAttributes textureAttributes = new TextureAttributes();
			//no attributes set now
			//tus.setTextureAttributes(textureAttributes);

			if (J3dNiGeometry.textureExists(fileName, textureSource))
			{
				Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);

				ImageComponent[] ics = tex.getImages();
				TextureCubeMap tcm = new TextureCubeMap(ics.length <= 1 ? Texture.BASE_LEVEL : Texture.MULTI_LEVEL_MIPMAP, Texture.RGBA,
						tex.getWidth());

				for (int f = 0; f < 6; f++)
					for (int l = 0; l < ics.length; l++)
						tcm.setImage(l, f, (ImageComponent2D) ics[l]);

				tus.setTexture(tcm);
				tus.setName(fileName);
			}
			else
			{
				System.out.println("bindCube BSLightingShaderProperty " + fileName + " No Texture found " + bslsp.nVer.fileName);
			}

			//setUpTimeController(ntp, niToJ3dData);	

			allTextureUnitStates.add(tus);
			uni1i(textureUnitName, texunit++);
		}
	}

	private void bind(String textureUnitName, NiTexturingProperty ntp, String fileName, int clamp)
	{
		if (programHasVar(textureUnitName))
		{
			TextureUnitState tus = new TextureUnitState();
			//TextureAttributes textureAttributes = new TextureAttributes();

			//TODO: jonwd7 suggest texture slot is the decaling place, see his fixed pipeline
			// also these should go through as shader uniforms I reckon
			//textureAttributes.setTextureMode(ntp.isApplyReplace() ? TextureAttributes.REPLACE
			//		: ntp.isApplyDecal() ? TextureAttributes.DECAL : TextureAttributes.MODULATE);

			//tus.setTextureAttributes(textureAttributes);

			if (J3dNiGeometry.textureExists(fileName, textureSource))
			{
				Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);
				tus.setTexture(tex);
				tus.setName(fileName);
			}
			else
			{
				System.out.println("NiTexturingProperty " + fileName + " No Texture found " + ntp.nVer.fileName);
			}

			//setUpTimeController(ntp, niToJ3dData);	

			allTextureUnitStates.add(tus);
			uni1i(textureUnitName, texunit++);
		}
	}

	private void bind(String textureUnitName, BSShaderLightingProperty bsprop, String fileName, int clamp)
	{
		if (programHasVar(textureUnitName))
		{
			TextureUnitState tus = new TextureUnitState();
			//TextureAttributes textureAttributes = new TextureAttributes();
			//no attributes set now
			//tus.setTextureAttributes(textureAttributes);

			if (J3dNiGeometry.textureExists(fileName, textureSource))
			{
				Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);
				tus.setTexture(tex);
				tus.setName(fileName);
			}
			else
			{
				System.out.println("BSShaderLightingProperty " + fileName + " No Texture found " + bsprop.nVer.fileName);
			}

			//setUpTimeController(bslsp, niToJ3dData);

			allTextureUnitStates.add(tus);
			uni1i(textureUnitName, texunit++);
		}
	}

	private void bind(String textureUnitName, BSLightingShaderProperty bslsp, String fileName, int clamp)
	{
		if (programHasVar(textureUnitName))
		{
			TextureUnitState tus = new TextureUnitState();

			if (J3dNiGeometry.textureExists(fileName, textureSource))
			{
				Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);
				tus.setTexture(tex);
				tus.setName(fileName);
			}
			else
			{
				System.out.println("BSLightingShaderProperty " + fileName + " No Texture found " + bslsp.nVer.fileName);
			}

			//NiSingleInterpController controller = (NiSingleInterpController) niToJ3dData.get(bslsp.controller);
			//setUpTimeController(controller, niToJ3dData);

			System.out.println("bound " + textureUnitName + " to " + texunit + " with " + fileName);
			allTextureUnitStates.add(tus);
			uni1i(textureUnitName, texunit++);

		}

	}

	private void bind(String textureUnitName, BSEffectShaderProperty bsesp, String fileName, int clamp)
	{
		if (programHasVar(textureUnitName))
		{
			TextureUnitState tus = new TextureUnitState();
			//TextureAttributes textureAttributes = new TextureAttributes();
			//no attributes set now
			//tus.setTextureAttributes(textureAttributes);
			if (J3dNiGeometry.textureExists(fileName, textureSource))
			{
				Texture tex = J3dNiGeometry.loadTexture(fileName, textureSource);
				tus.setTexture(tex);
				tus.setName(fileName);
			}
			else
			{
				System.out.println("BSEffectShaderProperty " + fileName + " No Texture found " + bsesp.nVer.fileName);
			}

			//setUpTimeController(bsesp, niToJ3dData);

			allTextureUnitStates.add(tus);
			uni1i(textureUnitName, texunit++);
		}
	}

	private boolean hasFileName(BSLightingShaderProperty bslsp, int textureSlot)
	{
		String fn = fileName(bslsp, textureSlot);
		return fn != null && fn.trim().length() > 0;
	}

	private String fileName(BSLightingShaderProperty bslsp, int textureSlot)
	{
		return fileName(bslsp, textureSlot, null);
	}

	private String fileName(BSLightingShaderProperty bslsp, int textureSlot, String defaultFileName)
	{
		if (bslsp != null)
		{

			// FO4 has material files pointed at by name
			if (bslsp.Name.toLowerCase().endsWith(".bgsm"))
			{
				// if the bgsm file exists the textureset may have bad .tga files in it (or good .dds ones)
				// but the bgsm definitely has good textures

				try
				{
					BgsmFile bgsm = BgsmSource.getBgsmFile(bslsp.Name);
					if (bgsm != null)
					{
						return bgsm.textures[textureSlot];
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else if (bslsp.TextureSet.ref != -1)
			{
				BSShaderTextureSet texSet = (BSShaderTextureSet) niToJ3dData.get(bslsp.TextureSet);
				return texSet.textures[textureSlot];
			}

		}
		return defaultFileName;

	}

	private String fileName(NiTexturingProperty ntp, int textureSlot)
	{
		//TODO: for now it appears that this is only EVER asking for 0 so let's just trap all others!
		if (textureSlot != 0)
		{
			new Throwable("Non 0 texSlot for TexturingProperty!").printStackTrace();
		}

		// now set the texture
		if (ntp != null && textureSlot == 0 && ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1)
		{
			NiSourceTexture niSourceTexture = (NiSourceTexture) niToJ3dData.get(ntp.baseTexture.source);
			return niSourceTexture.fileName.string;
		}

		return null;
	}

	private String fileName(BSShaderProperty bsprop, int textureSlot)
	{
		if (bsprop instanceof BSShaderPPLightingProperty)
		{
			BSShaderPPLightingProperty bsspplp = (BSShaderPPLightingProperty) bsprop;
			BSShaderTextureSet bbsts = (BSShaderTextureSet) niToJ3dData.get(bsspplp.textureSet);

			return bbsts.textures[textureSlot];
		}
		else
		{
			if (textureSlot == 0)
			{
				if (bsprop instanceof BSShaderNoLightingProperty)
				{
					BSShaderNoLightingProperty bssnlp = (BSShaderNoLightingProperty) bsprop;
					return bssnlp.fileName;
				}
				else if (bsprop instanceof TileShaderProperty)
				{
					TileShaderProperty tsp = (TileShaderProperty) bsprop;
					return tsp.fileName;
				}
				else if (bsprop instanceof TallGrassShaderProperty)
				{
					TallGrassShaderProperty tgsp = (TallGrassShaderProperty) bsprop;
					return tgsp.fileName;
				}
				else if (bsprop instanceof SkyShaderProperty)
				{
					SkyShaderProperty tsp = (SkyShaderProperty) bsprop;
					return tsp.fileName;
				}
				else if (bsprop instanceof WaterShaderProperty)
				{
					//WaterShaderProperty tsp = (WaterShaderProperty) bsprop;
					//TODO: water shader there!
				}
			}
			else
			{
				//System.out.println("is this an error or fine?");
			}
		}
		return null;
	}

}
