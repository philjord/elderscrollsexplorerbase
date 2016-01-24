#version 120 
//#version 120 is not optional, trouble otherwise

attribute vec4 glVertex;                
//attribute vec3 glNormal;     
//attribute vec4 glVertexColor;                
//attribute vec2 glMultiTexCoord0; 


uniform mat4 glProjectionMatrix;
uniform mat4 glModelViewMatrix;
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

uniform mat4 textureTransform;

varying vec2 glTexCoord0;

varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 N;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * gl_MultiTexCoord0).st;	
	//glTexCoord0 = (textureTransform * glMultiTexCoord0).st;

	N = normalize(glNormalMatrix * gl_Normal);
		
	vec3 v = vec3(glModelViewMatrix * glVertex);

	ViewDir = -v.xyz;
	LightDir = glLightSource0position.xyz;

	A = glLightModelambient;
	C = gl_Color;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;	
	
	
	//if(glVertex != gl_Vertex) C = vec4(1,0,1,1); 	
}
