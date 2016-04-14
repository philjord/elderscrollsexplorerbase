#version 120

attribute vec4 glVertex;  
attribute vec4 glColor;          
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialemission;
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

varying vec4 ColorEA;
varying vec4 ColorD;

varying vec3 N;
varying vec3 t;
varying vec3 b;
varying vec3 v;

varying vec4 A;
varying vec4 D;


void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,1)).xy ;	
	
/*	if(textureTransform[0][0] < 0.0
	||textureTransform[0][1] < 0.0
	||textureTransform[0][2] < 0.0
	||textureTransform[0][3] < 0.0
	||textureTransform[1][0] < 0.0
	||textureTransform[1][1] < 0.0
	||textureTransform[1][2] < 0.0
	||textureTransform[1][3] < 0.0
	||textureTransform[2][0] < 0.0
	||textureTransform[2][1] < 0.0
	||textureTransform[2][2] < 0.0
	||textureTransform[2][3] < 0.0
	||textureTransform[3][0] < 0.0
	||textureTransform[3][1] < 0.0
	||textureTransform[3][2] < 0.0
	||textureTransform[3][3] < 0.0
	)*/
	
	//if(glTexCoord0 !=   vec4(glMultiTexCoord0,0,1) )	
	//gl_Position = vec4(0,0,0,0);
	
	
	
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
	D = glLightSource0diffuse;
	
	if( ignoreVertexColors != 0 )
	{
		ColorEA = glFrontMaterialemission + glFrontMaterialdiffuse * A;
		ColorD = glFrontMaterialdiffuse * D;
	}
	else
	{
		ColorEA = glFrontMaterialemission + glColor * A;
		ColorD = glColor * D;
	}
}
