#version 120

attribute vec4 glVertex;         
attribute vec4 glColor;       
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

uniform mat4 textureTransform;
//End of FFP inputs
varying vec2 glTexCoord0;

attribute vec3 tangent;
attribute vec3 binormal;

varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 N;
varying vec3 t;
varying vec3 b;
varying vec3 v;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,1)).st;	
	
	N = normalize(glNormalMatrix * glNormal);
	t = normalize(glNormalMatrix * tangent);
	b = normalize(glNormalMatrix * binormal);
	
	// NOTE: b<->t 
	mat3 tbnMatrix = mat3(b.x, t.x, N.x,
                          b.y, t.y, N.y,
                          b.z, t.z, N.z);
						  
	v = vec3(glModelViewMatrix * glVertex);
	
	ViewDir = tbnMatrix * -v.xyz;
	LightDir = tbnMatrix * glLightSource0position.xyz;
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterialdiffuse; 
	else 
		C = glColor;
	D = glLightSource0diffuse;
}