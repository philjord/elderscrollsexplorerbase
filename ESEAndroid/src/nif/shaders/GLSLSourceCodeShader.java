package nif.shaders;

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
		String[] declarations = shaderSource.split(";");
		for (String codeLine : declarations)
		{
			if (codeLine.trim().startsWith("uniform"))
			{
				shaderUniformNames.add(codeLine.substring(codeLine.indexOf(" ", codeLine.indexOf(" ") + 1) + 1));
			}
			else if (codeLine.trim().startsWith("attribute"))
			{
				shaderVertexAttributeNames.add(codeLine.substring(codeLine.indexOf(" ", codeLine.indexOf(" ") + 1) + 1));
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
}
