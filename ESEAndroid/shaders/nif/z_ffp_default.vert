
varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 N;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	gl_Position = ftransform();
	gl_TexCoord[0] = gl_MultiTexCoord0;

	N = normalize(gl_NormalMatrix * gl_Normal);
		
	vec3 v = vec3(gl_ModelViewMatrix * gl_Vertex);


	ViewDir = -v.xyz;
	LightDir = gl_LightSource[0].position.xyz;

	A = gl_LightModel.ambient;
	C = gl_Color;
	D = gl_LightSource[0].diffuse * gl_FrontMaterial.diffuse;
}
