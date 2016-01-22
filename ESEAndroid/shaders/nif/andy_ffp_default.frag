#version 120

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;



uniform sampler2D BaseMap;

varying vec2 glTexCoord0;

varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 N;

varying vec4 A;
varying vec4 C;
varying vec4 D;



void main( void )
{
	vec4 baseMap = texture2D( BaseMap, glTexCoord0.st );
	
	if(alphaTestEnabled==0)
	{
		if(alphaTestFunction==512)
			discard;
		else if(alphaTestFunction==514)//==
			if(baseMap.a!=alphaTestValue)discard;
		else if(alphaTestFunction==517)//!=
			if(baseMap.a==alphaTestValue)discard;
		else if(alphaTestFunction==513)//<
			if(baseMap.a>=alphaTestValue)discard;
		else if(alphaTestFunction==515)//<=
			if(baseMap.a>alphaTestValue)discard;
		else if(alphaTestFunction==516)//>
			if(baseMap.a<=alphaTestValue)discard;
		else if(alphaTestFunction==518)//>=
			if(baseMap.a<alphaTestValue)discard;
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

	// Emissive
	vec3 emissive = gl_FrontMaterial.emission.rgb;

	// Specular
	vec3 spec = gl_FrontMaterial.specular.rgb * pow(NdotH, 0.3*gl_FrontMaterial.shininess);
	spec *= D.rgb;
	
	color.rgb = albedo * (diffuse + emissive) + spec.rgb;
	color.a = C.a * baseMap.a;

	gl_FragColor = color;
}
