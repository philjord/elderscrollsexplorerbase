#version 120

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

uniform int layerCount;

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
	
	if(layerCount>0)	albedo = (layer0alpha * texture2D( layerMap1, glTexCoord0.st ).rgb) + ((1-layer0alpha)*albedo);
	if(layerCount>1)	albedo = (layer1alpha * texture2D( layerMap2, glTexCoord0.st ).rgb) + ((1-layer1alpha)*albedo);
	if(layerCount>2)	albedo = (layer2alpha * texture2D( layerMap3, glTexCoord0.st ).rgb) + ((1-layer2alpha)*albedo);
	if(layerCount>3)	albedo = (layer3alpha * texture2D( layerMap4, glTexCoord0.st ).rgb) + ((1-layer3alpha)*albedo);
	if(layerCount>4)	albedo = (layer4alpha * texture2D( layerMap5, glTexCoord0.st ).rgb) + ((1-layer4alpha)*albedo);
	if(layerCount>5)	albedo = (layer5alpha * texture2D( layerMap6, glTexCoord0.st ).rgb) + ((1-layer5alpha)*albedo);
	if(layerCount>6)	albedo = (layer6alpha * texture2D( layerMap7, glTexCoord0.st ).rgb) + ((1-layer6alpha)*albedo);
	
	
	albedo = albedo * C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse ;
	color.a = 1;

	gl_FragColor = color;	
}