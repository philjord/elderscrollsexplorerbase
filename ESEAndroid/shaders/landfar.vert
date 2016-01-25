#version 120

attribute vec4 glVertex;
attribute vec4 glColor;

uniform mat4 glProjectionMatrix;
uniform mat4 glModelViewMatrix;
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


void main( void )
{			
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;

	A = glLightModelambient;
	if( ignoreVertexColors != 0 )
		C = glFrontMaterialdiffuse; 
	else
		C = glColor;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;

   	glTexCoord0 = gl_MultiTexCoord0.st; 
		  	
}
