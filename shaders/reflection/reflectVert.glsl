#version 330

layout(location = 0)in vec3 vertex;
layout(location = 1)in vec3 normal;
layout(location = 2)in vec2 uv;

/*layout(location = 3)in ivec4 boneIds[2];
layout(location = 5)in vec4 weights[2];
layout(location = 7)in int material;*/

uniform mat4 model = mat4(1), view, lightProj, lightView; 
uniform mat3 nMatrix = mat3(1);
uniform vec3 lightPos;

out vec4 shadowCoord;
out vec3 norm, lightVec;
out vec2 uvs;
flat out int matId;

void main(){
	mat4 shadowBias = mat4(vec4(.5,0,0,0), vec4(0,.5,0,0), vec4(0,0,.5,0), vec4(.5,.5,.5,1));
	
	
	vec4 vertInSpace = model*vec4(vertex, 1);
	lightVec = lightPos-vertInSpace.xyz;
	norm = nMatrix*normal;
	uvs = uv;
	matId = 0;
	
	shadowCoord = shadowBias*lightProj*lightView*vertInSpace;
	gl_Position = vertInSpace;
}