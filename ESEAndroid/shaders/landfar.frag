#version 120

precision mediump float;

uniform sampler2D baseMap;
 
varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec2 glTexCoord0;

void main( void )
{
	vec4 baseMapTex = texture2D( baseMap, glTexCoord0.st );
	
	vec3 albedo = baseMapTex.rgb;	
	
	albedo = albedo * C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse ;
	color.a = 1.0;

	gl_FragColor = color;	
}