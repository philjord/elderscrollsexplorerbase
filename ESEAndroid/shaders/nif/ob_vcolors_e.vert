#version 120

attribute vec4 glVertex;         
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform vec4 glFrontMaterialambient;
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
varying vec3 HalfVector;

varying vec4 ColorEA;
varying vec4 ColorD;

vec3 normal;
vec3 tangent;
vec3 binormal;

vec3 tspace( vec3 v )
{
	return vec3( dot( v, binormal ), dot( v, tangent ), dot( v, normal ) );
}

void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,0)).st;	
	
	normal = normalize(glNormalMatrix * glNormal);
	tangent = normalize(glNormalMatrix * tangent);
	binormal = normalize(glNormalMatrix * binormal);
	
	ViewDir = tspace( ( glModelViewMatrix * glVertex ).xyz );
	LightDir = tspace( glLightSource0position.xyz ); // light 0 is directional
	HalfVector = ( glModelViewMatrix * glVertex ).xyz - glLightSource0position.xyz;
	
	if(ignoreVertexColors != 0)
		ColorEA = glFrontMaterialdiffuse + glFrontMaterialambient * glLightModelambient;
	else
		ColorEA = glColor + glFrontMaterialambient * glLightModelambient;
	
	ColorD = glFrontMaterialdiffuse * glLightSource0.diffuse;
}
