package nif.shaders;

import javax.media.j3d.SourceCodeShader;

public class SourceCodeShader2 extends SourceCodeShader
{
	public SourceCodeShader2(int shadingLanguage, int shaderType, String shaderSource)
	{
		super(shadingLanguage, shaderType, shaderSource);
		this.shaderSource = shaderSource;
	}

	public String name = "";

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
