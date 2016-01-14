varying vec3 LightDir;
varying vec3 ViewDir;

varying vec3 N;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	N = normalize(gl_NormalMatrix * gl_Normal);
		
	vec3 v = vec3(gl_ModelViewMatrix * gl_Vertex);

	ViewDir = -v.xyz;
	LightDir = gl_LightSource[0].position.xyz;

	A = gl_LightModel.ambient;
	C = gl_Color;
	D = gl_LightSource[0].diffuse * gl_FrontMaterial.diffuse;

   	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;   	
		
	//these are just opacity values stored in s, t is 0
	gl_TexCoord[1] = gl_MultiTexCoord1;
	gl_TexCoord[2] = gl_MultiTexCoord2;
	gl_TexCoord[3] = gl_MultiTexCoord3;
	gl_TexCoord[4] = gl_MultiTexCoord4;
	gl_TexCoord[5] = gl_MultiTexCoord5;
	gl_TexCoord[6] = gl_MultiTexCoord6;
	gl_TexCoord[7] = gl_MultiTexCoord7;		
	
	gl_Position = ftransform();
	
}
