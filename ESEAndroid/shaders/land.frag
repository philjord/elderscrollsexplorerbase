#version 120
//https://www.khronos.org/files/opengles_shading_language.pdf
precision mediump float;

uniform sampler2D baseMap;
uniform sampler2D layerMap1;
uniform sampler2D layerMap2;
uniform sampler2D layerMap3;
uniform sampler2D layerMap4;
uniform sampler2D layerMap5;
uniform sampler2D layerMap6;
uniform sampler2D layerMap7;
uniform sampler2D layerMap8;
 
varying vec4 A;
varying vec4 C;
varying vec4 D;

//glsles requires highp for a shared uniform
uniform highp int layerCount;

varying vec2 glTexCoord0;

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
	vec4 baseMapTex = texture2D( baseMap, glTexCoord0.st );
	
	vec3 albedo = baseMapTex.rgb;		
	
	if(layerCount>0)	albedo = mix(albedo, texture2D( layerMap1, glTexCoord0.st ).rgb, layer0alpha);
	if(layerCount>1)	albedo = mix(albedo, texture2D( layerMap2, glTexCoord0.st ).rgb, layer1alpha);
	if(layerCount>2)	albedo = mix(albedo, texture2D( layerMap3, glTexCoord0.st ).rgb, layer2alpha);
	if(layerCount>3)	albedo = mix(albedo, texture2D( layerMap4, glTexCoord0.st ).rgb, layer3alpha);
	if(layerCount>4)	albedo = mix(albedo, texture2D( layerMap5, glTexCoord0.st ).rgb, layer4alpha);
	if(layerCount>5)	albedo = mix(albedo, texture2D( layerMap6, glTexCoord0.st ).rgb, layer5alpha);
	if(layerCount>6)	albedo = mix(albedo, texture2D( layerMap7, glTexCoord0.st ).rgb, layer6alpha);
	
	
	albedo = albedo * C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse ;
	color.a = 1.0;

	gl_FragColor = color;	
}