package nif.shaders;

import java.util.ArrayList;
import java.util.HashSet;

import javax.media.j3d.SourceCodeShader;

public class GLSLSourceCodeShader extends SourceCodeShader
{
	
	
	
	public HashSet<String> shaderUniformNames = new HashSet<String>();
	public HashSet<String> shaderVertexAttributeNames = new HashSet<String>();

	public String name = "";

	public GLSLSourceCodeShader(int shadingLanguage, int shaderType, String shaderSource)
	{
		super(shadingLanguage, shaderType, shaderSource);
		this.shaderSource = shaderSource;		
		

		//attempt to extract attribute names very poorly
		//start by discarding everything before //End of FFP inputs
		String allCode = shaderSource;
		if (allCode.contains("//End of FFP inputs"))
			allCode = allCode.substring(allCode.indexOf("//End of FFP inputs") + "//End of FFP inputs".length());

		String[] declarations = allCode.split("\n");
		for (String codeLine : declarations)
		{
			// chuck away any comment parts at the end of the line
			if (codeLine.contains("//"))
				codeLine = codeLine.substring(0, codeLine.indexOf("//"));
			// TODO: cross fingers there is nothing in /**/ style comments

			if (codeLine.trim().startsWith("uniform"))
			{
				// find start of name after type
				String line = codeLine.substring(codeLine.indexOf(" ", codeLine.indexOf(" ") + 1) + 1);
				// drop final ;
				line = line.replace(";", "").trim();
				shaderUniformNames.add(line);
			}
			else if (codeLine.trim().startsWith("attribute"))
			{
				// find start of name after type
				String line = codeLine.substring(codeLine.indexOf(" ", codeLine.indexOf(" ") + 1) + 1);
				// drop final ;
				line = line.replace(";", "").trim();
				shaderVertexAttributeNames.add(line);
			}
		}
	}

	public String toString()
	{
		return "SourceCodeShader2: " + name;
	}

	private String shaderSource = "";

	public String getShaderSource()
	{
		return shaderSource;
	}

	public boolean shaderHasVar(String var)
	{
		return shaderSource.contains(" " + var + ";");
	}

	//TODO: MUCH improve this checking, convert type to glsl and check white space properly
	public boolean shaderHasVar(String var, String type)
	{
		//System.out.println("" + this + " checked for " + var + " " + type + " = " + (shaderSource.contains(" " + var + ";")));
		return shaderSource.contains(" " + var + ";");
	}

	public boolean shaderHasVar(String var, String type, int arrSize)
	{
		return shaderSource.contains(arrSize + " " + var + ";");
	}

	public static String[][] replacements = new String[][] { //			
			{ "gl_ProjectionMatrix", "glProjectionMatrix" }, //
			{ "gl_ProjectionMatrixInverse", "glProjectionMatrixInverse" }, //
			{ "gl_ModelViewMatrix", "glModelViewMatrix" }, //
			{ "gl_ModelViewMatrixInverse", "glModelViewMatrixInverse" }, //
			{ "gl_ModelViewProjectionMatrix", "glModelViewProjectionMatrix" }, //
			{ "gl_Vertex", "glVertex" }, //
			{ "gl_Normal", "glNormal" }, //
			{ "gl_Color", "glColor" }, //
			{ "gl_SecondaryColor", "No option research" }, //
			{ "gl_TextureMatrix", "textureTransform with no texture units" },
			{ "gl_MultiTexCoord", "glMultiTexCoord* where * is texture unit number" }, //
			{ "gl_TexCoord", "manual varying now (like glTexCoord0)" }, //		
			{ "gl_FrontColor", "manual varying now" }, //			
			{ "gl_FogCoord", "glFogCoord not sure about this yet" }, //
			{ "gl_LightSource", "glLightSource** eg glLightSource0diffuse (only 0 for now!)" }, //
			{ "gl_FrontMaterial", "glFrontMaterial* eg glFrontMaterialshininess" }, //
			{ "gl_BackMaterial", "No option research" }, //
			{ "gl_FrontLightModelProduct", "No option research" }, //
			{ "gl_BackLightModelProduct", "No option research" }, //
	};

	/**
	 * list of suggested replacements
	 * 
	 */
	public static ArrayList<String> testForFFP(String ss)
	{
		ArrayList<String> ret = new ArrayList<String>();
		/*	ss = ss.replace("gl_FragColor", "");
			ss = ss.replace("gl_Position", "");
			ss = ss.replace("gl_FrontFacing", "");
			ss = ss.replace("gl_PointSize", "");
			ss = ss.replace("gl_FragCoord", "");
			ss = ss.replace("gl_PointCoord", "");
			ss = ss.replace("gl_FragData", "");
			ss = ss.replace("gl_DepthRange", "");
			ss = ss.replace("gl_MaxVertexAttribs", "");
			ss = ss.replace("gl_MaxVertexUniformVectors", "");
			ss = ss.replace("gl_MaxVaryingVector", "");
			ss = ss.replace("gl_MaxVertexTextureImageUnits", "");
			ss = ss.replace("gl_MaxCombinedTextureImageUnits", "");
			ss = ss.replace("gl_MaxTextureImageUnits", "");
			ss = ss.replace("gl_MaxFragmentUniformVectors", "");
			ss = ss.replace("gl_MaxDrawBuffers", "");
		
			if (ss.contains("gl_"))
				System.out.println("Looks like FFP code!!! " + ss);*/

		for (int i = 0; i < replacements.length; i++)
		{
			String[] rep = replacements[i];
			if (ss.contains(rep[0]))
				ret.add(rep[0] + " should be replaced with " + rep[1]);
		}
		return ret;

	}
}
