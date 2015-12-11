#version 120

uniform sampler2D BaseMap;

varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 N;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	vec4 baseMap = texture2D( BaseMap, gl_TexCoord[0].st );

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
