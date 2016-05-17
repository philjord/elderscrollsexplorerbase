#version 120

attribute vec4 glVertex;
attribute vec4 glColor;
attribute vec2 glMultiTexCoord0; 

attribute vec4 samplers0;
attribute vec4 samplers1;
attribute vec4 samplers2;
attribute vec4 samplers3;

uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;
uniform mat4 glProjectionMatrix;
//uniform mat4 glModelViewMatrix;
//uniform mat4 glModelViewProjectionMatrix;
//uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

varying vec2 glTexCoord0;

varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec4 fragSamplers0;
varying vec4 fragSamplers1;
varying vec4 fragSamplers2;
varying vec4 fragSamplers3;
 

void main( void )
{	
mat4 glModelViewMatrix = glViewMatrix*glModelMatrix;
	gl_Position = glProjectionMatrix*glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;

	glTexCoord0 = glMultiTexCoord0.st; 
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0 ) 
		C = glFrontMaterialdiffuse; 
	else 
		C = glColor;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;
	 
	
	fragSamplers0 = samplers0;
	fragSamplers1 = samplers1;
	fragSamplers2 = samplers2; 	
	fragSamplers3 = samplers3; 	
	//TODO: I could work out the right ones here now and pass the samplers across to the frag as varying
	// or maybe not need to check
	
}
