#version 140

precision mediump float;

layout ( std140, shared ) uniform FFP_Uniform_Block
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
};

/*uniform int alphaTestEnabled2;
uniform int alphaTestFunction2;
uniform float alphaTestValue2;*/
//End of FFP inputs
in vec2 glTexCoord0;

uniform sampler2D BaseMap;

in vec3 LightDir;
in vec3 ViewDir;

in vec3 N;

in vec4 A;
in vec4 C;
in vec4 D;


in vec3 emissive;
in vec3 specular;
in float shininess;

out vec4 glFragColor;

void main( void )
{
	vec4 baseMap = texture( BaseMap, glTexCoord0.st );
	
	//web says the keyword discard in a shader is bad
	//I could just gl_FragColor=vec(0,0,0,0); return;
	if(alphaTestEnabled != 0)
	{				
	 	if(alphaTestFunction==516)//>
			if(baseMap.a<=alphaTestValue)discard;			
		else if(alphaTestFunction==518)//>=
			if(baseMap.a<alphaTestValue)discard;		
		else if(alphaTestFunction==514)//==
			if(baseMap.a!=alphaTestValue)discard;
		else if(alphaTestFunction==517)//!=
			if(baseMap.a==alphaTestValue)discard;
		else if(alphaTestFunction==513)//<
			if(baseMap.a>=alphaTestValue)discard;
		else if(alphaTestFunction==515)//<=
			if(baseMap.a>alphaTestValue)discard;		
		else if(alphaTestFunction==512)//never	
			discard;			
	}

	vec3 normal = N;
	
	vec3 L = normalize(LightDir);
	vec3 E = normalize(ViewDir);
	vec3 R = reflect(-L, normal);
	vec3 H = normalize( L + E );
	
	float NdotL = max( dot(normal, L), 0.0 );
	float NdotH = max( dot(normal, H), 0.0 );
	float EdotN = max( dot(normal, E), 0.0 );
	float NdotNegL = max( dot(normal, -L), 0.0 );

	vec4 color;
	vec3 albedo = baseMap.rgb * C.rgb;
	vec3 diffuse = A.rgb + (D.rgb * NdotL);


	// Specular
	vec3 spec = specular * pow(NdotH, 0.3*shininess);
	spec *= D.rgb;
	
	color.rgb = albedo * (diffuse + emissive) + spec;
	color.a = C.a * baseMap.a;

	glFragColor = color;
}
