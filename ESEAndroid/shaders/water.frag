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

uniform int fogEnabled;
uniform vec4 expColor;
uniform float expDensity;
uniform vec4 linearColor;
uniform float linearStart;
uniform float linearEnd;
 
varying vec3 ViewDir;

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
     
    if(fogEnabled == 1)
	{
		//distance
		float dist = 0.0;
		float fogFactor = 0.0;
		 
		//compute distance used in fog equations
		dist = length(ViewDir);		 
		 
		if(linearEnd > 0.0)//linear fog
		{
		   fogFactor = (linearEnd - dist)/(linearEnd - linearStart);
		   fogFactor = clamp( fogFactor, 0.0, 1.0 );
		 
		   //if you inverse color in glsl mix function you have to put 1.0 - fogFactor
		   color = mix(linearColor, color, fogFactor);		 
		}
		else if( expDensity > 0.0)// exponential fog
		{
		    fogFactor = 1.0 /exp(dist * expDensity);
		    fogFactor = clamp( fogFactor, 0.0, 1.0 );
		 
		    // mix function fogColor-(1-fogFactor) + lightColor-fogFactor
		    color = mix(expColor, color, fogFactor);
		}
		
		af = fogFactor*2.0;// terrible but there  you go.
	}
	
		 
     
	ct = color.rgb * C.rgb;
	at = color.a;
	gl_FragColor = vec4(ct * cf, at * af);
}