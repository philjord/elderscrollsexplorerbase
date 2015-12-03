package nif.shaders;

import javax.media.j3d.ShaderAttributeValue;

public class ShaderAttributeValue2 extends ShaderAttributeValue
{
	public ShaderAttributeValue2(String attrName, Object value)
	{
		super(attrName, value);
	}

	public String toString()
	{
		return "SourceCodeShader2: " + this.getAttributeName() + " " + this.getValue();
	}
}
