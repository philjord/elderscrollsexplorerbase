#version 150 

in vec4 glVertex;         
in vec4 glColor;       
in vec3 glNormal;     
in vec2 glMultiTexCoord0; 

uniform mat4 glProjectionMatrix;
//uniform mat4 glProjectionMatrixInverse;
uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;
//uniform mat4 glModelViewMatrix;
//uniform mat4 glModelViewMatrixInverse;
//uniform mat4 glModelViewProjectionMatrix;
				
//uniform mat3 glNormalMatrix;

uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

struct material
{
	int lightEnabled;
 	vec4 ambient;
 	vec4 diffuse;
 	vec4 emission;
 	vec3 specular;
 	float shininess;
};
uniform material glFrontMaterial;

struct lightSource
{
	 vec4 position;
	 vec4 diffuse;
	 vec4 specular;
	 float constantAttenuation, linearAttenuation, quadraticAttenuation;
	 float spotCutoff, spotExponent;
	 vec3 spotDirection;
};

uniform int numberOfLights;// numberOfLights will be set to how many the pipeline can send
//NOTE android might support a very low number of varying attributes as low as 8
const int maxLights = 3;// this is for the shader, it will process no more than this, must be a const
uniform lightSource glLightSource[maxLights];

uniform mat4 textureTransform;

out vec2 glTexCoord0;

out vec3 ViewVec;
out vec4 C;
out vec3 light;


void main( void )
{
	mat4 glModelViewMatrix = glViewMatrix*glModelMatrix;// calculated here to reduce transer from CPU
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0,1)).st;		

	mat3 glNormalMatrix =  mat3(transpose(inverse(glModelViewMatrix)));
	vec3 N = normalize(glNormalMatrix * glNormal);
		
	vec3 v = vec3(glModelViewMatrix * glVertex);
	ViewVec = -v.xyz;// do not normalize also used for view dist	

	vec4 A = glLightModelambient *  glFrontMaterial.ambient;
			 
	if( ignoreVertexColors != 0) 
	{
		// objectColor should be used if it is no lighting, and reusing material diffuse appears wrong
		C = vec4(1,1,1,1);//glFrontMaterialdiffuse;
	}
	else 
		C = glColor; 
		
	vec3 emissive = glFrontMaterial.emission.rgb;
	float shininess = glFrontMaterial.shininess;
	
	vec3 diffuse = A.rgb;
	vec3 spec = vec3(0,0,0);
	
	vec3 normal = N;
	vec3 E = normalize(ViewVec);
	float EdotN = max( dot(normal, E), 0.0 );
	
	vec4 vertPos = glModelMatrix * glVertex;
	
	for (int index = 0; index < numberOfLights && index < maxLights; index++) // for all light sources
	{ 		
		vec4 Lp = glLightSource[index].position;
		vec3 Ld = normalize( glLightSource[index].position.xyz );		
		//vec3 R = reflect(-L, normal);
		vec3 H = normalize( Ld + E );
			
		float NdotL = max( dot(normal, Ld), 0.0 );
		float NdotH = max( dot(normal, H), 0.0 );		
		float NdotNegL = max( dot(normal, -Ld), 0.0 );		
		
		vec3 d = ((glLightSource[index].diffuse * glFrontMaterial.diffuse).rgb * NdotL);
		vec3 s = ((glLightSource[index].specular.rgb * glFrontMaterial.specular) * pow(NdotH, 0.3*shininess));
    
		// Attenuate the light based on distance. but not for directional!
		if(Lp.w == 1.0)
		{
   			float dist = length(vertPos - glLightSource[index].position);   
   			float att = (1.0 / (glLightSource[index].constantAttenuation +
				(glLightSource[index].linearAttenuation*dist) +
					(glLightSource[index].quadraticAttenuation*dist*dist)));    
   			d = d * att;
   			s = s * att; 
    	}			 
		
		diffuse = diffuse + d;
        spec = spec + s;		
 
	}	
	
	light = (diffuse + emissive) + spec;	
	 
}
