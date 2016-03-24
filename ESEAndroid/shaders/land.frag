#version 120
//https://www.khronos.org/files/opengles_shading_language.pdf
precision mediump float;

uniform sampler2D baseMap;

uniform sampler2D layerMap0;
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
//uniform highp int layerCount;

varying vec2 glTexCoord0;

varying vec4 layerAlpha4;
varying vec4 layerAlpha8;
varying vec4 layerAlpha12;

void main( void )
{
	vec4 baseMapTex = texture2D( baseMap, glTexCoord0.st );
	
	vec3 albedo = baseMapTex.rgb;		
	
	albedo = mix(albedo, texture2D( layerMap0, glTexCoord0.st ).rgb, layerAlpha4.x);
	albedo = mix(albedo, texture2D( layerMap1, glTexCoord0.st ).rgb, layerAlpha4.y);
	albedo = mix(albedo, texture2D( layerMap2, glTexCoord0.st ).rgb, layerAlpha4.z);
	albedo = mix(albedo, texture2D( layerMap3, glTexCoord0.st ).rgb, layerAlpha4.w);
	albedo = mix(albedo, texture2D( layerMap4, glTexCoord0.st ).rgb, layerAlpha8.x);
	albedo = mix(albedo, texture2D( layerMap5, glTexCoord0.st ).rgb, layerAlpha8.y);
	albedo = mix(albedo, texture2D( layerMap6, glTexCoord0.st ).rgb, layerAlpha8.z);
	albedo = mix(albedo, texture2D( layerMap7, glTexCoord0.st ).rgb, layerAlpha8.w);
	albedo = layerAlpha12.x > 0.0 ? mix(albedo, texture2D( layerMap8, glTexCoord0.st ).rgb, layerAlpha12.x) : albedo;
	
	albedo = albedo * C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse ;
	color.a = 1.0;

	gl_FragColor = color;	
	//gl_FragColor = baseMapTex;
	//gl_FragColor = vec4(float(layerCount)/6.0,layerAlpha4.x,layerAlpha4.y,1);
 
}