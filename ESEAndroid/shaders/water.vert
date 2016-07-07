#version 120


attribute vec4 glVertex;
attribute vec4 glColor;
attribute vec3 glNormal;  
attribute vec2 glMultiTexCoord0;

//uniform mat4 glProjectionMatrix;
uniform mediump mat4 glProjectionMatrixInverse;
uniform mediump mat4 glModelViewMatrix;
uniform mediump mat4 glModelViewProjectionMatrix;
uniform mediump mat3 glNormalMatrix;


uniform vec4 glFrontMaterialdiffuse;
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

uniform vec4 glLightSource0position;
uniform vec4 glLightSource0diffuse;

varying vec2 glTexCoord0;

const float pi = 3.14159;
uniform float waterHeight;
uniform float time;
uniform int numWaves;
uniform float amplitude[8];
uniform float wavelength[8];
uniform float speed[8];
uniform vec2 direction[8];
varying vec3 position;
varying vec3 worldNormal;
varying vec3 eyeNormal;
varying vec3 lightDir;

varying vec3 ViewDir;

varying vec4 A;
varying vec4 C;
varying vec4 D;

float wave(int i, float x, float z) {
    float frequency = 2.0*pi/wavelength[i];
    float phase = speed[i] * frequency;
    float theta = dot(direction[i], vec2(x, z));
    return amplitude[i] * sin(theta * frequency + time * phase);
}

float waveHeight(float x, float z) {
// I need to update this to be deterministic on x,z and time,
// that way each side of adjacent grids will match
    float height = 0.0;
    for (int i = 0; i < numWaves; ++i)
        height += wave(i, x, z);
    return height;
}

float dWavedx(int i, float x, float z) {
    float frequency = 2.0*pi/wavelength[i];
    float phase = speed[i] * frequency;
    float theta = dot(direction[i], vec2(x, z));
    float A = amplitude[i] * direction[i].x * frequency;
    return A * cos(theta * frequency + time * phase);
}

float dWavedz(int i, float x, float z) {
    float frequency = 2.0*pi/wavelength[i];
    float phase = speed[i] * frequency;
    float theta = dot(direction[i], vec2(x, z));
    float A = amplitude[i] * direction[i].y * frequency;
    return A * cos(theta * frequency + time * phase);
}

vec3 waveNormal(float x, float z) {
    float dx = 0.0;
    float dz = 0.0;
    for (int i = 0; i < numWaves; ++i) {
        dx += dWavedx(i, x, z);
        dz += dWavedz(i, x, z);
    }
    vec3 n = vec3(-dx, -dz, 1.0);
    return normalize(n);
}

void main() {
    vec4 pos = glVertex;
    pos.y = pos.y;// turned off for demo + waveHeight(pos.x, pos.z);
    position = pos.xyz / pos.w;
    worldNormal = waveNormal(pos.x, pos.z);
    eyeNormal = glNormalMatrix * worldNormal;
    glTexCoord0 = glMultiTexCoord0.st;
    gl_Position = glModelViewProjectionMatrix * pos;
    
    vec3 v = vec3(glModelViewMatrix * glVertex);
	ViewDir = -v.xyz;

    
    lightDir = normalize(vec3(glLightSource0position));    
    vec4 P = glModelViewMatrix * glVertex;
	vec4 E = glProjectionMatrixInverse * vec4(0.0,0.0,1.0,0.0);
	vec3 I = P.xyz*E.w - E.xyz*P.w;
	vec3 N = glNormalMatrix * glNormal;
	vec3 Nf = normalize(faceforward(N,I,N));

	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterialdiffuse; 
	else
		C = glColor;
	D = glLightSource0diffuse * glFrontMaterialdiffuse;		

/*	for (int i=0; i<gl_MaxLights; i++)
	{
		vec3 L = normalize(gl_LightSource[i].position.xyz*P.w -
			P.xyz*gl_LightSource[i].position.w);
		gl_FrontColor.xyz +=
			gl_LightSource[i].ambient.xyz +
			gl_LightSource[i].diffuse.xyz*max(dot(Nf,L),0.);
	}    */
	 	
}
