#version 120 
//#version 120 is not optional, trouble otherwise

attribute vec4 glVertex;         
attribute vec4 glColor;       
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 


uniform mat4 glProjectionMatrix;
uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform vec3 glFrontMaterialemission;
uniform vec3 glFrontMaterialspecular;
uniform float glFrontMaterialshininess;
uniform int ignoreVertexColors;

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

varying vec3 emissive;
varying vec3 specular;
varying float shininess;

void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,0)).st;	

	N = normalize(glNormalMatrix * glNormal);
		
	vec3 v = vec3(glModelViewMatrix * glVertex);

	ViewDir = -v.xyz;
	LightDir = glLightSource0position.xyz;

	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterialdiffuse; 
	else 
		C = glColor;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;		
	
	emissive = glFrontMaterialemission;
	specular = glFrontMaterialspecular;
	shininess = glFrontMaterialshininess;
	
}
