package nif.appearance;

import javax.media.j3d.Appearance;
import javax.media.j3d.Shape3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiGeometry;
import nif.shaders.NiGeometryAppearanceShader;
import utils.source.TextureSource;

public class NiGeometryAppearanceFactoryOverride implements NiGeometryAppearance
{
	private static NiGeometryAppearanceFixed niGeometryAppearanceFixed = null;
	private static NiGeometryAppearanceFactoryOverride NiGeomteryAppearanceFactory = null;

	public static NiGeometryAppearance getNiGeometryAppearance()
	{
		if (niGeometryAppearanceFixed == null)
		{
			niGeometryAppearanceFixed = new NiGeometryAppearanceFixed();
			NiGeomteryAppearanceFactory = new NiGeometryAppearanceFactoryOverride();
		}
		return NiGeomteryAppearanceFactory;
	}

	@Override
	public Appearance configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D shape,
			J3dNiAVObject target)
	{
		NiGeometryAppearanceShader niGeometryAppearanceShader = new NiGeometryAppearanceShader(textureSource, niToJ3dData);
		niGeometryAppearanceShader.setupShaderProgram(niGeometry, niToJ3dData, null);
		Appearance app = niGeometryAppearanceShader.getAppearance();
		// note time controllers below need appearance set on the shape now
		//TODO: set the time controllers too!
		shape.setAppearance(app);
		return app;
	}

}
