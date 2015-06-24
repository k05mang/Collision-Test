#version 330

layout(location = 0)in vec3 vertex;

uniform mat4 model, view, proj; 

void main(){
	gl_Position = proj*view*model*vec4(vertex, 1);
}