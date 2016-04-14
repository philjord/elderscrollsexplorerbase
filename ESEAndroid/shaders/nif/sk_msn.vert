#version 120

attribute vec4 glVertex;         
attribute vec4 glColor;       
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

uniform mat4 textureTransform;
//End of FFP inputs
varying vec2 glTexCoord0;

varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 v;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,1)).st;
							  
	v = vec3(glModelViewMatrix * glVertex);

	ViewDir = -v.xyz;
	LightDir = glLightSource0position.xyz;
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterialdiffuse; 
	else 
		C = glColor;
	D = glLightSource0diffuse;
}