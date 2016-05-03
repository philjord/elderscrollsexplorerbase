#version 140 
//#version 120 is not optional, trouble otherwise

//Note don't put if else constructs on one line or trouble

in vec4 glVertex;         
in vec4 glColor;       
in vec3 glNormal;     
in vec2 glMultiTexCoord0; 

/*layout ( std140, shared ) uniform FFP_Uniform_Block
{
 mat4 glProjectionMatrix;
 mat4 glProjectionMatrixInverse;
 mat4 glViewMatrix;
 mat4 glModelMatrix;
 mat4 glModelViewMatrix;
 mat4 glModelViewMatrixInverse;
 mat4 glModelViewProjectionMatrix;				
 mat3 glNormalMatrix;

 vec4 glFrontMaterialdiffuse;
 vec4 glFrontMaterialemission;
 vec3 glFrontMaterialspecular;
 float glFrontMaterialshininess;
 
 int ignoreVertexColors;
 
 vec4 glLightModelambient;
 vec4 objectColor;
  
 vec4 glLightSource0position;
 vec4 glLightSource0diffuse;

 mat4 textureTransform;
 
 int alphaTestEnabled;
 int alphaTestFunction;
 float alphaTestValue;
};*/


//uniform mat4 glProjectionMatrix;
//uniform mat4 glProjectionMatrixInverse;
//uniform mat4 glViewMatrix;
//uniform mat4 glModelMatrix;
uniform mat4 glModelViewMatrix;
//uniform mat4 glModelViewMatrixInverse;
uniform mat4 glModelViewProjectionMatrix;
				
uniform mat3 glNormalMatrix;

uniform vec4 glFrontMaterialdiffuse;
uniform vec4 glFrontMaterialemission;
uniform vec3 glFrontMaterialspecular;
uniform float glFrontMaterialshininess;
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

uniform mat4 textureTransform;
 
//uniform int alphaTestEnabled;
//uniform int alphaTestFunction;
//uniform float alphaTestValue;

//End of FFP inputs

//The line above in not optional for parsing reasons

out vec2 glTexCoord0;

out vec3 LightDir;
out vec3 ViewDir;

out vec3 N;

out vec4 A;
out vec4 C;
out vec4 D;

out vec3 emissive;
out vec3 specular;
out float shininess;

void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,1)).st;		

	N = normalize(glNormalMatrix * glNormal);
		
	vec3 v = vec3(glModelViewMatrix * glVertex);

	ViewDir = -v.xyz;
	LightDir = glLightSource0position.xyz;

	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterialdiffuse; // objectColor should be used if it is no lighting
	else 
		C = glColor; 
	D = glLightSource0diffuse * glFrontMaterialdiffuse;		
	
	emissive = glFrontMaterialemission.rgb;
	specular = glFrontMaterialspecular;
	shininess = glFrontMaterialshininess;
	
	 
}
