#version 120

attribute vec4 glVertex;
attribute vec4 glColor;
attribute vec2 glMultiTexCoord0; 
attribute vec2 glMultiTexCoord1; 
attribute vec2 glMultiTexCoord2; 
attribute vec2 glMultiTexCoord3; 
attribute vec2 glMultiTexCoord4; 
attribute vec2 glMultiTexCoord5; 
attribute vec2 glMultiTexCoord6; 
attribute vec2 glMultiTexCoord7; 

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

uniform int layerCount;


varying vec4 A;
varying vec4 C;
varying vec4 D;

varying float layer0alpha;
varying float layer1alpha;
varying float layer2alpha;
varying float layer3alpha;
varying float layer4alpha;
varying float layer5alpha;
varying float layer6alpha;
varying float layer7alpha;

void main( void )
{			
	gl_Position = glModelViewProjectionMatrix * glVertex;

	A = glLightModelambient;
	if( ignoreVertexColors != 0 ) 
		C = glFrontMaterialdiffuse; 
	else 
		C = glColor;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;

   	glTexCoord0 = glMultiTexCoord0.st;   	
		
		//TODO: I should preload 1- alpha int vec2 t
	//these are just opacity values stored in s, t is 0
	if(layerCount>0)	layer0alpha = glMultiTexCoord1.s;
	if(layerCount>1)	layer1alpha = glMultiTexCoord2.s;
	if(layerCount>2)	layer2alpha = glMultiTexCoord3.s;
	if(layerCount>3)	layer3alpha = glMultiTexCoord4.s;
	if(layerCount>4)	layer4alpha = glMultiTexCoord5.s;
	if(layerCount>5)	layer5alpha = glMultiTexCoord6.s;
	if(layerCount>6)	layer6alpha = glMultiTexCoord7.s;	
	
	 	
		 
}
