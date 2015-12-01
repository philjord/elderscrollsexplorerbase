package nif.shaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.GLSLShaderProgram;
import javax.media.j3d.Material;
import javax.media.j3d.NodeComponent;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shader;
import javax.media.j3d.ShaderAppearance;
import javax.media.j3d.ShaderAttribute;
import javax.media.j3d.ShaderAttributeSet;
import javax.media.j3d.ShaderAttributeValue;
import javax.media.j3d.ShaderProgram;
import javax.media.j3d.SourceCodeShader;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TextureUnitState;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.sun.j3d.utils.shader.StringIO;

import nif.BgsmSource;
import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifColor3;
import nif.compound.NifMatrix33;
import nif.compound.NifMatrix44;
import nif.compound.NifTexCoord;
import nif.compound.NifTexDesc;
import nif.enums.BSLightingShaderPropertyShaderType;
import nif.enums.BSShaderFlags;
import nif.enums.BSShaderType;
import nif.enums.FaceDrawMode;
import nif.enums.SkyrimShaderPropertyFlags1;
import nif.enums.SkyrimShaderPropertyFlags2;
import nif.enums.TexClampMode;
import nif.enums.VertMode;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiDitherProperty;
import nif.niobject.NiFogProperty;
import nif.niobject.NiGeometry;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiMultiTextureProperty;
import nif.niobject.NiObject;
import nif.niobject.NiProperty;
import nif.niobject.NiShadeProperty;
import nif.niobject.NiSourceTexture;
import nif.niobject.NiSpecularProperty;
import nif.niobject.NiStencilProperty;
import nif.niobject.NiTextureModeProperty;
import nif.niobject.NiTextureProperty;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiVertexColorProperty;
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
import nif.niobject.bs.BSSkyShaderProperty;
import nif.niobject.bs.BSWaterShaderProperty;
import nif.niobject.bs.Lighting30ShaderProperty;
import nif.niobject.bs.SkyShaderProperty;
import nif.niobject.bs.TallGrassShaderProperty;
import nif.niobject.bs.TileShaderProperty;
import nif.niobject.bs.WaterShaderProperty;
import nif.niobject.controller.NiSingleInterpController;
import utils.convert.NifOpenGLToJava3D;
import utils.source.TextureSource;

/**
 this will build an appearance up out of a NiGeometry that can be used by a real j3dnigeometry It is based on(copied from?)the nifskope 2.0 renderer code from jonwd7

*/

public class J3dNiGeometryShader
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

	private ShaderProgram shaderProgram = null;

	private ShaderAttributeSet shaderAttributeSet = null;

	private HashSet<ShaderAttributeValue> allShaderAttributeValues = new HashSet<ShaderAttributeValue>();
	private HashSet<TextureUnitState> allTextureUnitStates = new HashSet<TextureUnitState>();

	public J3dNiGeometryShader(TextureSource textureSource, NiToJ3dData niToJ3dData)
	{
		this.textureSource = textureSource;

		//configure app defaults	
		mat.setLightingEnable(true);
		mat.setColorTarget(Material.AMBIENT_AND_DIFFUSE);
		mat.setAmbientColor(new Color3f(0.4f, 0.4f, 0.4f));
		mat.setDiffuseColor(new Color3f(0.8f, 0.8f, 0.8f));
		mat.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
		//TODO: this 128 business of java3d seems arbitrary, test removal (and below too)
		mat.setShininess(0.33f * 128);//jonwd7 says so
		app.setMaterial(mat);

		//MUST always be set on everything (stencils!)
		app.setRenderingAttributes(ra);

		app.setPolygonAttributes(pa);

		app.setTransparencyAttributes(ta);

	}

	public static Texture loadTexture(String texName, TextureSource ts)
	{
		if (ts != null && texName != null && texName.length() > 0)
		{
			// morrowind has bmp and tga endings ?
			texName = texName.toLowerCase().trim();
			if (texName.endsWith(".bmp"))
				texName = texName.substring(0, texName.indexOf(".bmp")) + ".dds";
			else if (texName.endsWith(".tga"))
				texName = texName.substring(0, texName.indexOf(".tga")) + ".dds";

			return ts.getTexture(texName);
		}

		return null;
	}

	class Property
	{

	}

	class QModelIndex
	{

	}

	class NifModel
	{

	}

	class Shape
	{

	}

	boolean setupProgram(ShaderProgarms.Program prog, NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props)
	{
		if (!prog.conditions.eval(niGeometry, niToJ3dData))
			return false;

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

		int texunit = 0;

		String textureUnitName = "BaseMap";
		if (texprop != null)
		{
			bind(texprop, fileName(texprop, 0), clamp, texunit);
		}
		else if (bsprop != null)
		{
			bind(bsprop, fileName(bsprop, 0), clamp, texunit);
		}
		else if (bslsp != null)
		{
			bind(bslsp, fileName(bslsp, 0), clamp, texunit);
		}
		uni1i(textureUnitName, texunit++);

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

			bind(texprop, fname, clamp, texunit);
		}
		else if (bsprop != null)
		{
			bind(bsprop, fileName(bsprop, 1), clamp, texunit);
		}
		else if (bslsp != null)
		{
			bind(bslsp, fileName(bslsp, 1), clamp, texunit);
		}
		uni1i(textureUnitName, texunit++);

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

			bind(texprop, fname, clamp, texunit);
		}
		else if (bsprop != null)
		{
			bind(bsprop, fileName(bsprop, 2), clamp, texunit);
		}
		else if (bslsp != null)
		{
			bind(bslsp, fileName(bslsp, 2), clamp, texunit);
		}
		uni1i(textureUnitName, texunit++);

		String white = "shaders/nif/white.dds";
		String black = "shaders/nif/black.dds";
		String default_n = "shaders/nif/default_n.dds";

		// BSLightingShaderProperty
		if (bslsp != null)
		{
			BSShaderTextureSet texSet = (BSShaderTextureSet) niToJ3dData.get(bslsp.TextureSet);

			uni1f("lightingEffect1", bslsp.LightingEffect1);
			uni1f("lightingEffect2", bslsp.LightingEffect2);

			uni1f("alpha", bslsp.Alpha);

			//TODO: 	no change sign as shader does what it likes?? shaders?	
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

			uni1i("hasTintColor", bslsp.HairTintColor != null || bslsp.SkinTintColor != null ? 1 : 0);
			if (bslsp.HairTintColor != null || bslsp.SkinTintColor != null)
			{
				NifColor3 tC = bslsp.HairTintColor != null ? bslsp.HairTintColor : bslsp.SkinTintColor;
				uni3f("tintColor", tC.r, tC.g, tC.b);
			}

			boolean hasTintMask = bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_FaceTint;
			boolean hasDetailMask = hasTintMask;
			uni1i("hasDetailMask", hasDetailMask);
			if (hasDetailMask)
			{
				bind(bslsp, fileName(bslsp, 3, "shaders/nif/blankdetailmap.dds"), clamp, texunit);
				uni1i("DetailMask", texunit++);
			}

			uni1i("hasTintMask", hasTintMask);
			if (hasTintMask)
			{
				bind(bslsp, fileName(bslsp, 6, "shaders/nif/gray.dds"), clamp, texunit);
				uni1i("TintMask", texunit++);
			}

			// Rim & Soft params
			boolean hasSoftlight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Soft_Lighting);
			boolean hasRimlight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Rim_Lighting);
			uni1i("hasSoftlight", hasSoftlight);
			uni1i("hasRimlight", hasRimlight);
			if (hasSoftlight || hasRimlight)
			{
				bind(bslsp, fileName(bslsp, 2, default_n), clamp, texunit);
				uni1i("LightMask", texunit++);
			}

			// Backlight params
			boolean hasBacklight = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Back_Lighting);
			uni1i("hasBacklight", hasBacklight);
			if (hasBacklight)
			{
				bind(bslsp, fileName(bslsp, 7, default_n), clamp, texunit);
				uni1i("BacklightMap", texunit++);
			}

			// Glow params

			boolean hasEmittance = bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Own_Emit);
			uni1i("hasEmit", hasEmittance);
			if (hasEmittance)
				uni1f("glowMult", bslsp.EmissiveMultiple);
			else
				uni1f("glowMult", 0);

			boolean hasGlowMap = bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Glow_Map) && fileName(bslsp, 2) != null;
			uni1i("hasGlowMap", hasGlowMap);

			uni3f("glowColor", bslsp.EmissiveColor.r, bslsp.EmissiveColor.g, bslsp.EmissiveColor.g);

			// Specular params

			uni1f("specStrength", bslsp.SpecularStrength);

			// Assure specular power does not break the shaders
			float gloss = bslsp.Glossiness;
			uni1f("specGlossiness", (gloss > 0.0) ? gloss : 1.0f);

			uni3f("specColor", bslsp.SpecularColor.r, bslsp.SpecularColor.g, bslsp.SpecularColor.b);

			boolean hasSpecularMap = bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Specular) && fileName(bslsp, 7) != null;
			uni1i("hasSpecularMap", hasSpecularMap);

			if (hasSpecularMap && !hasBacklight)
			{
				bind(bslsp, fileName(bslsp, 7, default_n), clamp, texunit);
				uni1i("SpecularMap", texunit++);
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

				bind(bslsp, fileName(bslsp, 6, default_n), clamp, texunit);
				uni1i("InnerMap", texunit++);
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
					&& fileName(bslsp, 4) != null;

			uni1i("hasCubeMap", hasCubeMap);

			boolean useEnvironmentMask = hasEnvironmentMap && fileName(bslsp, 5) != null;

			uni1i("hasEnvMask", useEnvironmentMask);

			if (hasCubeMap && hasEnvironmentMap)
			{
				float envReflection = 0;
				if (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EnvironmentMap)
					envReflection = bslsp.EnvironmentMapScale;
				else if (bslsp.SkyrimShaderType.type == BSLightingShaderPropertyShaderType.ST_EyeEnvmap)
					envReflection = bslsp.EyeCubemapScale;

				uni1f("envReflection", envReflection);

				bind(bslsp, fileName(bslsp, 5, white), clamp, texunit);
				uni1i("EnvironmentMap", texunit++);

				bindCube(bslsp, fileName(bslsp, 4), texunit);
				uni1i("CubeMap", texunit++);
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
			hasHeightMap |= bslsp.ShaderFlags1.isBitSet(SkyrimShaderPropertyFlags1.SLSF1_Parallax) && fileName(bslsp, 3) != null;

			if (hasHeightMap)
			{
				bind(bslsp, fileName(bslsp, 3, "shaders/nif/gray.dds"), clamp, texunit);
				uni1i("HeightMap", texunit++);
			}
		}

		BSEffectShaderProperty bsesp = (BSEffectShaderProperty) props.get(BSEffectShaderProperty.class);
		if (niGeometry.nVer.LOAD_USER_VER2 < 130 && bsesp != null)
		{

			clamp = bsesp.TextureClampMode.mode;
			clamp = clamp ^ TexClampMode.MIRRORED_S_MIRRORED_T;

			bind(bsesp, bsesp.SourceTexture, clamp, texunit);
			uni1i("SourceTexture", texunit++);

			boolean isDoubleSided = bsesp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Double_Sided);
			uni1i("doubleSided", isDoubleSided);

			uni2f("uvScale", bsesp.UVScale.u, bsesp.UVScale.v);

			uni2f("uvOffset", bsesp.UVOffSet.u, bsesp.UVOffSet.v);

			uni1i("hasSourceTexture", bsesp.SourceTexture != null);
			uni1i("hasGreyscaleMap", bsesp.GreyscaleTexture != null);

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
			if (bsesp.GreyscaleTexture != null)
			{
				bind(bsesp, bsesp.GreyscaleTexture, TexClampMode.MIRRORED_S_MIRRORED_T, texunit);
				uni1i("GreyscaleMap", texunit++);
			}
		}

		// Defaults for uniforms in older meshes
		if (bsesp == null && bslsp == null)
		{
			uni2f("uvScale", 1.0f, 1.0f);
			uni2f("uvOffset", 0.0f, 0.0f);
		}

		//TODO: below he is attaching vertex attributes
		// the transTangents are for morphable meshes, so they operate exactly like coords
		// so for me these are normal texcoord indexes, from .vert
		//N = normalize(gl_NormalMatrix * gl_Normal);
		//t = normalize(gl_NormalMatrix * gl_MultiTexCoord1.xyz);
		//b = normalize(gl_NormalMatrix * gl_MultiTexCoord2.xyz);
		//I need to set them (obviously) but I also need to be able to morph them(which is a freebie cos they
		// the same as coords)

		// interleave handles this fine!

		// because it's ALWAYS in the prog file , I'll just detect not that and warn, otherwise this lot is in the
		// J3dNiTriBasedGeom code in all cases
		//texcoords 0 base
		//texcoords 1 tangents
		//texcoords 2 bitangents

		//  FO4 bitangents:
		//auto t = nif->get<ByteVector3>( idx, "Tangent" );
		//tangents += t;
		//auto b = Vector3::crossproduct( n, t );
		//bitangents += Vector3( dot, unk1f, unk2f );
		for (Map.Entry<Integer, String> itx : prog.texcoords.entrySet())
		{

			// this just my texcoords index, no probs
			//map type one(can I use maps?) 
			if (!activateTextureUnit(itx.getKey()))
				return false;

			if (itx.getValue() == "tangents")
			{
				if (mesh.tangents.count())
				{
					glEnableClientState(GL_TEXTURE_COORD_ARRAY);
					glTexCoordPointer(3, GL_FLOAT, 0, mesh.tangents.data());
				}
				else
				{
					return false;
				}

			}
			else if (itx.getValue() == "bitangents")
			{
				if (mesh.bitangents.count())
				{
					glEnableClientState(GL_TEXTURE_COORD_ARRAY);
					glTexCoordPointer(3, GL_FLOAT, 0, mesh.bitangents.data());
				}
				else
				{
					return false;
				}
			}
			else if (texprop != null)
			{
				int txid = ShaderProgarms.TexturingPropertygetId(itx.getValue());
				if (txid < 0)
					return false;

				int set = texprop.coordSet(txid);

				if (set < 0 || !(set < mesh.coords.count()) || !mesh.coords[set].count())
					return false;

				glEnableClientState(GL_TEXTURE_COORD_ARRAY);
				glTexCoordPointer(2, GL_FLOAT, 0, mesh.coords[set].data());
			}
			else if (bsprop != null)
			{
				int txid = ShaderProgarms.BSShaderLightingPropertygetId(itx.getValue());
				if (txid < 0)
					return false;

				int set = 0;

				if (set < 0 || !(set < mesh.coords.count()) || !mesh.coords[set].count())
					return false;

				glEnableClientState(GL_TEXTURE_COORD_ARRAY);
				glTexCoordPointer(2, GL_FLOAT, 0, mesh.coords[set].data());
			}
		}

		shaderProgram = prog.shaderProgram;
		shaderAttributeSet = new ShaderAttributeSet();

		String[] shaderAttrNames = new String[allShaderAttributeValues.size()];
		int i = 0;
		for (ShaderAttributeValue sav : allShaderAttributeValues)
		{
			shaderAttrNames[i++] = sav.getName();
			shaderAttributeSet.put(sav);
		}

		shaderProgram.setShaderAttrNames(shaderAttrNames);

		Shader[] shaders = prog.fileShaders.values().toArray(new Shader[] {});
		shaderProgram.setShaders(shaders);

		TextureUnitState[] tus = allTextureUnitStates.toArray(new TextureUnitState[] {});

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

	private void bindCube(BSLightingShaderProperty bslsp, String fileName, int texunit)
	{
		// TODO Auto-generated method stub
		System.out.println("bindCube I feel not?");
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

			if (nsp != null)
			{
				if ((nsp.flags.flags & 0x01) != 0)
				{
					mat.setShininess(nmp.glossiness * 128);
					mat.setSpecularColor(nmp.specularColor.r, nmp.specularColor.g, nmp.specularColor.b);
				}
				else
				{
					mat.setShininess(0);
					mat.setSpecularColor(0, 0, 0);
				}
			}
		}

	}

	private void glProperty(NiAlphaProperty nap)
	{
		if (nap != null)
		{
			if (nap.alphaBlendingEnable())
			{
				ta.setTransparencyMode(TransparencyAttributes.BLENDED);
				ta.setSrcBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.sourceBlendMode(), true));
				ta.setDstBlendFunction(NifOpenGLToJava3D.convertBlendMode(nap.destinationBlendMode(), false));
			}
			else
			{
				ta.setTransparencyMode(TransparencyAttributes.SCREEN_DOOR);
			}

			if (nap.alphaTestEnabled())
			{
				// I think the PolygonAttributes.CULL_NONE should be applied to anything
				// with an alphaTestEnabled(), flat_lod trees from skyrim prove it
				// obviously transparent stuff can be seen from the back quite often
				pa.setCullFace(PolygonAttributes.CULL_NONE);
				pa.setBackFaceNormalFlip(true);

				int alphaTestMode = NifOpenGLToJava3D.convertAlphaTestMode(nap.alphaTestMode());
				ra.setAlphaTestFunction(alphaTestMode);

				float threshold = ((nap.threshold) / 255f);// threshold range of 255 to 0 comfirmed
															// empirically
				ra.setAlphaTestValue(threshold);
			}

		}

	}

	// Sets a float
	private void uni1f(String var, float x)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Float(x)));
	}

	// Sets a vec2 (two floats)
	private void uni2f(String var, float x, float y)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Vector2f(x, y)));
	}

	// Sets a vec3 (three floats)
	private void uni3f(String var, float x, float y, float z)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Vector3f(x, y, z)));
	}

	// Sets a vec4 (four floats)
	private void uni4f(String var, float x, float y, float z, float w)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Vector4f(x, y, z, w)));
	};

	// Sets a boolean
	private void uni1i(String var, boolean val)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Integer(val ? 1 : 0)));
	};

	// Sets an integer  
	private void uni1i(String var, int val)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Integer(val)));
	};

	// Sets a mat3 (3x3 matrix)
	private void uni3m(String var, NifMatrix33 val)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Matrix3f(val.data())));
	}

	// Sets a mat4 (4x4 matrix)
	private void uni4m(String var, NifMatrix44 val)
	{
		allShaderAttributeValues.add(new ShaderAttributeValue(var, new Matrix4f(val.data())));
	};

	private void bind(NiTexturingProperty ntp, String fileName, int clamp, int texunit)
	{
		TextureUnitState tus = new TextureUnitState();
		TextureAttributes textureAttributes = new TextureAttributes();

		//TODO: jonwd7 suggest texture slot is the decaling place, see his fixed pipeline
		textureAttributes.setTextureMode(ntp.isApplyReplace() ? TextureAttributes.REPLACE
				: ntp.isApplyDecal() ? TextureAttributes.DECAL : TextureAttributes.MODULATE);

		tus.setTextureAttributes(textureAttributes);

		Texture tex = loadTexture(fileName, textureSource);
		tus.setTexture(tex);

		//setUpTimeController(ntp, niToJ3dData);	

		allTextureUnitStates.add(tus);
	}

	private void bind(BSShaderLightingProperty bsprop, String fileName, int clamp, int texunit)
	{
		TextureUnitState tus = new TextureUnitState();
		TextureAttributes textureAttributes = new TextureAttributes();
		//no attributes set now
		tus.setTextureAttributes(textureAttributes);

		Texture tex = loadTexture(fileName, textureSource);
		tus.setTexture(tex);

		//setUpTimeController(bslsp, niToJ3dData);

		allTextureUnitStates.add(tus);
	}

	private void bind(BSLightingShaderProperty bslsp, String fileName, int clamp, int texunit)
	{
		TextureUnitState tus = new TextureUnitState();
		TextureAttributes textureAttributes = new TextureAttributes();

		//TODO: check this jonwd7 does not do it
		// apparently the The vertex colors are used as well, just not the alpha component when
		// SF_Vertex_Animation is present
		// http://niftools.sourceforge.net/forum/viewtopic.php?f=10&t=3276
		if (bslsp.ShaderFlags2.isBitSet(SkyrimShaderPropertyFlags2.SLSF2_Tree_Anim))
		{
			textureAttributes.setTextureMode(TextureAttributes.COMBINE);
			textureAttributes.setCombineAlphaMode(TextureAttributes.COMBINE_REPLACE);
		}
		tus.setTextureAttributes(textureAttributes);

		Texture tex = loadTexture(fileName, textureSource);
		tus.setTexture(tex);

		//NiSingleInterpController controller = (NiSingleInterpController) niToJ3dData.get(bslsp.controller);
		//setUpTimeController(controller, niToJ3dData);

		allTextureUnitStates.add(tus);

	}

	private void bind(BSEffectShaderProperty bslsp, String fileName, int clamp, int texunit)
	{
		TextureUnitState tus = new TextureUnitState();
		TextureAttributes textureAttributes = new TextureAttributes();
		//no attributes set now
		tus.setTextureAttributes(textureAttributes);

		Texture tex = loadTexture(fileName, textureSource);
		tus.setTexture(tex);

		//setUpTimeController(bslsp, niToJ3dData);

		allTextureUnitStates.add(tus);
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

	private void setupFixedFunction(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props)
	{
		//morrowind in particular
		//TODO: fixedfunction tells me a lot about decals etc
	}

	public String setupShaderProgram(NiGeometry niGeometry, NiToJ3dData niToJ3dData, String hint)
	{
		ShaderProgarms.loadShaderPrograms();

		PropertyList props = new PropertyList(niGeometry.properties, niToJ3dData);

		/*if (  (mesh.scene.options & Scene.DisableShaders) || (mesh.scene.visMode & Scene.VisSilhouette) )
		{
			setupFixedFunction( mesh, props );
			return  "fixed function pipeline" ;
		}*/

		if (hint != null)
		{
			ShaderProgarms.Program program = ShaderProgarms.programs.get(hint);

			if (program != null && program.status && setupProgram(program, niGeometry, niToJ3dData, props))
				return hint;
		}

		for (ShaderProgarms.Program program : ShaderProgarms.programs.values())
		{
			if (program.status && setupProgram(program, niGeometry, niToJ3dData, props))
				return program.name;
		}

		setupFixedFunction(niGeometry, niToJ3dData, props);
		return "fixed function pipeline";
	}

	class PropertyList extends ArrayList<NiObject>
	{

		public PropertyList(NifRef[] properties, NiToJ3dData niToJ3dData)
		{
			for (int i = 0; i < properties.length; i++)
			{
				NiObject prop = niToJ3dData.get(properties[i]);
				if (prop != null)
				{
					add(prop);
				}
			}
		}

		public NiProperty get(Class<? extends NiProperty> type)
		{
			for (NiObject p : this)
			{
				if (type.isInstance(p))
					return (NiProperty) p;
			}
			return null;
		}

		public BSLightingShaderProperty getBSLightingShaderProperty()
		{
			for (NiObject p : this)
			{
				if (p instanceof BSLightingShaderProperty)
					return (BSLightingShaderProperty) p;
			}
			return null;
		}

	}

	class Scene
	{
		public static final boolean VisNormalsOnly = false;
		public static final boolean DoLighting = true;
		public static final boolean DoCubeMapping = true;
		public static final boolean DoGlow = true;
		public static final boolean DoSpecular = true;
	}

}
