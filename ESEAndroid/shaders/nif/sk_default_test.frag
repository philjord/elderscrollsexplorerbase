#version 120

uniform sampler2D BaseMap;
uniform sampler2D NormalMap;

uniform vec2 uvScale;
uniform vec2 uvOffset;

varying vec4 diffuse,ambientGlobal, ambient, ecPos;

varying vec3 normal,halfVector;
 
void main()
{
	vec2 offset = gl_TexCoord[0].st * uvScale + uvOffset;
 	vec4 baseMap = texture2D( BaseMap, offset );
 	vec4 normalMap = texture2D( NormalMap, offset );

    vec3 n,halfV,viewV,lightDir;
    float NdotL,NdotHV;
    vec4 color = ambientGlobal;
    float att, dist;
     
   
    // a fragment shader can't write a verying variable, hence we need
    //a new variable to store the normalized interpolated normal 
    //n = normalize(normal);
    n = normalize(normalMap.rgb * 2.0 - 1.0);
       
    // Compute the ligt direction
    lightDir = vec3(gl_LightSource[0].position-ecPos);
     
    // compute the distance to the light source to a varying variable
    dist = length(lightDir);
 
     
    // compute the dot product between normal and ldir 
    NdotL = max(dot(n,normalize(lightDir)),0.0);
 
    if (NdotL > 0.0) {
     
        att = 1.0 / (gl_LightSource[0].constantAttenuation +
                gl_LightSource[0].linearAttenuation * dist +
                gl_LightSource[0].quadraticAttenuation * dist * dist);
        color += att * (diffuse * NdotL + ambient);
     
         
        halfV = normalize(halfVector);
        NdotHV = max(dot(n,halfV),0.0);
        color += att * gl_FrontMaterial.specular * gl_LightSource[0].specular * pow(NdotHV,gl_FrontMaterial.shininess);
    }
    
    vec3 albedo = baseMap.rgb * color.rgb;
  	color.rgb = albedo;
    gl_FragColor = color;
}