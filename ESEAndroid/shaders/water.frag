#version 120

precision mediump float;

uniform mediump mat4 glProjectionMatrixInverse;

uniform int isCubeMap;

uniform samplerCube envMap;
uniform sampler2D tex;

varying vec3 position;
varying vec3 worldNormal;
varying vec3 eyeNormal;

varying vec3 lightDir;

varying vec2 glTexCoord0;

varying vec4 A;
varying vec4 C;
varying vec4 D;

void main() 
{ 
	vec4 color;
	if( isCubeMap == 1 )
	{
     	vec3 eyePos = vec3(glProjectionMatrixInverse * vec4(0,0,1,0));
     	vec3 eye = normalize(eyePos - position);
     	vec3 r = reflect(eye, worldNormal);   
     	color = vec4(0.0);//not working on android?? does not run shader textureCube(envMap, r); 
    }   
    else 
    {
		color = texture2D(tex, glTexCoord0.st);
    }     
     
    float intensity,at,af;
     vec3 ct,cf;
     intensity = max(dot(lightDir,normalize(eyeNormal)),0.0); 
     cf = intensity * D.rgb + A.rgb;
     af = D.a;	 
     
     ct = color.rgb * C.rgb;
     at = color.a;
     gl_FragColor = vec4(ct * cf, at * af);
}