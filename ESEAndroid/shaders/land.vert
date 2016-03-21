#version 120

attribute vec4 glVertex;
attribute vec4 glColor;
attribute vec2 glMultiTexCoord0; 

attribute vec4 alphas04;
attribute vec4 alphas58;
attribute vec4 alphas912;


uniform mat4 glProjectionMatrix;
uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

varying vec2 glTexCoord0;

varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec4 layerAlpha4;
varying vec4 layerAlpha8;
varying vec4 layerAlpha12;
 

void main( void )
{	
	gl_Position = glModelViewProjectionMatrix * glVertex;
	glTexCoord0 = glMultiTexCoord0.st; 
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0 ) 
		C = glFrontMaterialdiffuse; 
	else 
		C = glColor;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;
	 
	
	layerAlpha4 = alphas04;
	layerAlpha8 = alphas58;
	layerAlpha12 = alphas912; 	
	
}
