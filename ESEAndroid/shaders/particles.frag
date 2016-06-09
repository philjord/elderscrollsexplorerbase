#version 140

precision mediump float;
 
//End of FFP inputs
in vec2 glTexCoord0;

uniform sampler2D BaseMap;
 
in mediump vec2 TextureSize;

out vec4 glFragColor;


void main( void )
{	 
	//mediump vec2 realTexCoord = glTexCoord0 + (gl_PointCoord * TextureSize);
	mediump vec2 realTexCoord = gl_PointCoord;
    mediump vec4 fragColor = texture(BaseMap, realTexCoord ); 

    glFragColor = fragColor;	 
}
