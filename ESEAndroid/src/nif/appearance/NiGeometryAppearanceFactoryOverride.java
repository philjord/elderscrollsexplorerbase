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
	private static NiGeometryAppearanceFactoryOverride NiGeomteryAppearanceFactory = null;

	public static NiGeometryAppearance getNiGeometryAppearance()
	{
		if (NiGeomteryAppearanceFactory == null)
		{
			NiGeomteryAppearanceFactory = new NiGeometryAppearanceFactoryOverride();
		}
		return NiGeomteryAppearanceFactory;
	}

	@Override
	public Appearance configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D shape,
			J3dNiAVObject target)
	{
		NiGeometryAppearanceShader niGeometryAppearanceShader = new NiGeometryAppearanceShader(textureSource, niToJ3dData);
		String progName = niGeometryAppearanceShader.setupShaderProgram(niGeometry, niToJ3dData, null);
		if (progName != null)
		{
			Appearance app = niGeometryAppearanceShader.getAppearance();
			// note time controllers below need appearance set on the shape now
			//TODO: set the time controllers too!
			shape.setAppearance(app);

			return app;
		}
		else
		{
			if (NiGeometryAppearanceShader.OUTPUT_BINDINGS)
				System.out.println("using FFP");
			return new NiGeometryAppearanceFixed().configureAppearance(niGeometry, niToJ3dData, textureSource, shape, target);
		}
	}

}
