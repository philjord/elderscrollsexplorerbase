package scrollsexplorer.simpleclient.scenegraph;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.Background;
import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.GLSLShaderProgram;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.RenderingAttributes;
import org.jogamp.java3d.Shader;
import org.jogamp.java3d.ShaderAppearance;
import org.jogamp.java3d.SourceCodeShader;
import org.jogamp.java3d.Texture;
import org.jogamp.java3d.TriangleStripArray;
import org.jogamp.java3d.utils.geometry.Primitive;
import org.jogamp.java3d.utils.geometry.Sphere;
import org.jogamp.java3d.utils.shader.SimpleShaderAppearance;

import scrollsexplorer.GameConfig;
import tools3d.utils.ShaderSourceIO;
import tools3d.utils.Utils3D;
import utils.source.MediaSources;

public class SimpleSky extends BranchGroup
{
	private GameConfig gameConfig;

	private BranchGroup backSkyBG = new BranchGroup();

	/**
	  * Create some Background geometry to use as
	  * a backdrop for the application. Here we create
	  * a Sphere that will enclose the entire scene and
	  * apply a texture image onto the inside of the Sphere
	  * to serve as a graphical backdrop for the scene.
	  */
	public SimpleSky(GameConfig gameConfig1, MediaSources mediaSources)
	{
		this.gameConfig = gameConfig1;

		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		backSkyBG.setCapability(BranchGroup.ALLOW_DETACH);

		// create a new Background node
		Background backSky = new Background();

		// set the range of influence of the background
		backSky.setApplicationBounds(Utils3D.defaultBounds);

		// create a BranchGroup that will hold
		// our Sphere geometry
		BranchGroup bgGeometry = new BranchGroup();

		// create an appearance for the Sphere
		ShaderAppearance app = makeAppearance();

		Texture tex = null;
		// load a texture image 		
		tex = mediaSources.getTextureSource().getTexture(gameConfig.skyTexture);

		// apply the texture to the Appearance
		app.setTexture(tex);

		// create the Sphere geometry with radius 1.0.
		// we tell the Sphere to generate texture coordinates
		// to enable the texture image to be rendered
		// and because we are *inside* the Sphere we have to generate 
		// Normal coordinates inwards or the Sphere will not be visible.
		Sphere sphere = new Sphere(1.0f, Primitive.GENERATE_TEXTURE_COORDS | Primitive.GENERATE_NORMALS | Primitive.GENERATE_NORMALS_INWARD,
				8, app);

		// start wiring everything together,

		//Make in NIO
		TriangleStripArray geo = (TriangleStripArray) sphere.getShape().getGeometry();
		int[] stripVertexCounts = new int[geo.getNumStrips()];
		geo.getStripVertexCounts(stripVertexCounts);
		TriangleStripArray newGeo = new TriangleStripArray(geo.getVertexCount(), GeometryArray.COORDINATES | GeometryArray.NORMALS
				| GeometryArray.TEXTURE_COORDINATE_2 | GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE, stripVertexCounts);

		float[] coords = new float[geo.getValidVertexCount() * 3];
		geo.getCoordinates(0, coords);
		newGeo.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(coords)));
		float[] norms = new float[geo.getValidVertexCount() * 3];
		geo.getNormals(0, norms);
		newGeo.setNormalRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(norms)));
		float[] textcoords = new float[geo.getValidVertexCount() * 2];
		geo.getTextureCoordinates(0, 0, textcoords);
		newGeo.setTexCoordRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(textcoords)));

		newGeo.setName("Sky Geo");
		sphere.getShape().setGeometry(newGeo);

		// add the Sphere to its parent BranchGroup.
		bgGeometry.addChild(sphere);

		// assign the BranchGroup to the Background as geometry.
		backSky.setGeometry(bgGeometry);

		// add the Background node to its parent BranchGroup.
		backSkyBG.addChild(backSky);

		addChild(backSkyBG);

		/*Background background = new Background();
		background.setApplicationBounds(Utils3D.defaultBounds);
		//background.setColor(new Color3f(1.0f, 1.0f, 1.0f));
		
		BranchGroup bgNifbg = new BranchGroup();
		NifFile nifFile = NifToJ3d.loadNiObjects("meshes\\sky\\stars.nif", meshSource);
		NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
		for (NiObject no : nifFile.blocks)
		{
			if (no instanceof NiTriShape)
			{
				NiTriShape niTriShape = (NiTriShape) no;
				//J3dNiTriShape jnts = new J3dNiTriShape(niTriShape, niToJ3dData, textureSource);
				//bgNifbg.addChild(jnts);
			}
		}
		
		background.setGeometry(bgNifbg);
		
		BranchGroup bgbg = new BranchGroup();
		bgbg.addChild(background);*/

	}

	public void setShowSky(boolean showSky)
	{
		//TODO: note ! not on structure behavior, trouble?
		if (showSky)
		{
			if (!backSkyBG.isLive())
			{
				addChild(backSkyBG);
			}
		}
		else
		{
			if (backSkyBG.isLive())
			{
				backSkyBG.detach();
			}
		}

	}

	private static ShaderAppearance makeAppearance()
	{
		ShaderAppearance app = new SimpleShaderAppearance();
		app.setMaterial(null);
		app.setRenderingAttributes(new RenderingAttributes());

		String vertexProgram = ShaderSourceIO.getTextFileAsString("shaders/sky.vert");
		String fragmentProgram = ShaderSourceIO.getTextFileAsString("shaders/sky.frag");

		Shader[] shaders = new Shader[2];
		shaders[0] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_VERTEX, vertexProgram) {
			@Override
			public String toString()
			{
				return "vertexProgram";
			}
		};
		shaders[1] = new SourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, Shader.SHADER_TYPE_FRAGMENT, fragmentProgram) {
			@Override
			public String toString()
			{
				return "fragmentProgram";
			}
		};

		GLSLShaderProgram shaderProgram = new GLSLShaderProgram() {
			@Override
			public String toString()
			{
				return "Sky Shader Program";
			}
		};
		shaderProgram.setShaders(shaders);
		shaderProgram.setShaderAttrNames(new String[] { "BaseMap" });

		app.setShaderProgram(shaderProgram);
		return app;
	}
}
