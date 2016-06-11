#version 150 
//#version 120 is not optional, trouble otherwise

//Note don't put if else constructs on one line or trouble

in vec4 glVertex;         
in vec4 glColor;       
//in vec3 glNormal;     
in vec2 glMultiTexCoord0; 

uniform mat4 glProjectionMatrix;
//uniform mat4 glProjectionMatrixInverse;
uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;
	
uniform mat4 textureTransform;
//End of FFP inputs

in float Size;

out vec4 C;

// The size of the sprite being rendered. My sprites are square
// so I'm just passing in a float.  For non-square sprites pass in
// the width and height as a vec2.
//uniform float TextureCoordPointSize;

//out vec2 glTexCoord0;
//out vec2 TextureSize;

void main( void )
{
	mat4 glModelViewMatrix = glViewMatrix * glModelMatrix;
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;
	
	//glTexCoord0 = glMultiTexCoord0;
	//TextureSize = vec2(TextureCoordPointSize, TextureCoordPointSize);
	
	vec3 v = vec3(glModelViewMatrix * glVertex);
	vec3 ViewDir = -v.xyz;
	float dist = length(ViewDir);
			
	gl_PointSize = Size / dist;	 
	
	//TODO: must also hand in a rotation
	
	
	C = glColor; 
	
}
