#version 330

layout(triangles) in;
layout(triangle_strip, max_vertices=18) out;

in vec4 shadowCoord[];
in vec3 norm[], lightVec[];
in vec2 uvs[];
flat in int matId[];

uniform mat4 projection, cams[6];

out vec3 normal, lightVector;
out vec4 shadCoord;
out vec2 uv;
flat out int material;

void main(){

	for(int layer = 0; layer < 6; layer++){
		gl_Layer = layer;
		mat4 mvp = projection*cams[layer];
		for(int primitive = 0; primitive < 3;primitive++){
			gl_Position = mvp*gl_in[primitive].gl_Position;
			
			normal = norm[primitive];
			lightVector = lightVec[primitive];
			shadCoord = shadowCoord[primitive];
			uv = uvs[primitive];
			material = matId[primitive];
			EmitVertex();
		}
		EndPrimitive();
	}
}