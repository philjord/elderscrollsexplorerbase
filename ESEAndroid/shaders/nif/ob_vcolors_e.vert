
attribute vec3 tangent;
attribute vec3 binormal;

varying vec3 LightDir;
varying vec3 ViewDir;
varying vec3 HalfVector;

varying vec4 ColorEA;
varying vec4 ColorD;

vec3 normal;
vec3 tangent;
vec3 binormal;

vec3 tspace( vec3 v )
{
	return vec3( dot( v, binormal ), dot( v, tangent ), dot( v, normal ) );
}

void main( void )
{
	gl_Position = ftransform();
	gl_TexCoord[0] = gl_MultiTexCoord0;
	
	normal = normalize(gl_NormalMatrix * gl_Normal);
	tangent = normalize(gl_NormalMatrix * tangent);
	binormal = normalize(gl_NormalMatrix * binormal);
	
	ViewDir = tspace( ( gl_ModelViewMatrix * gl_Vertex ).xyz );
	LightDir = tspace( gl_LightSource[0].position.xyz ); // light 0 is directional
	HalfVector = tspace( gl_LightSource[0].halfVector.xyz );
	
	ColorEA = gl_Color + gl_FrontMaterial.ambient * gl_LightModel.ambient;
	ColorD = gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
}
