#version 330

in vec3 normal, lightVector;
in vec4 shadCoord;
in vec2 uv;
flat in int material;

uniform vec3 color;
uniform sampler2DShadow image;

uniform float cutOff, atten, intensity;
uniform vec3 lightDir;

out vec4 finalColor;

void main(){
	vec3 norm = normalize(normal);
	vec3 lightNorm = normalize(lightVector);
	float shading = textureProj(image, vec4(shadCoord.xy,(shadCoord.z-.0005),shadCoord.w));
	
	if(dot(lightDir, -lightNorm) > cutOff){
		float distance = length(lightVector);
		float attenuation = 1.0/(1+atten*(distance*distance));
		vec3 diffuse = max(dot(norm, lightNorm), .35)*color;
		finalColor = vec4( max(shading*attenuation*diffuse, .35*color),1);
	}else{
		finalColor = vec4( .35*color,1);
	}
}