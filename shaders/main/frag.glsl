#version 330

in vec4 shadowCoord;
in vec3 norm, lightVec, viewVec;
in vec2 uvs;
flat in int matId;

uniform vec3 color;
uniform sampler2DShadow image;
uniform samplerCube refMap;

uniform float cutOff, atten, intensity, specPower, specIntensity;
uniform vec3 lightDir;
uniform bool reflection;

out vec4 finalColor;

float computeNormalLighting(vec3 lightNorm, vec3 normal, float shadowFact, float ambient){
	float result;
	if(dot(lightDir, -lightNorm) > cutOff){
		float distance = length(lightVec);
		float attenuation = 1.0/(1+atten*(distance*distance));
		float diffuse = max(dot(normal, lightNorm), ambient);
		result = max(shadowFact*attenuation*diffuse, ambient);
	}else{
		result = ambient;
	}
	return result;
}

void main(){
	vec3 normal = normalize(norm);
	vec3 lightNorm = normalize(lightVec);
	vec3 view = normalize(viewVec);
	float shadowFact = textureProj(image, vec4(shadowCoord.xy,(shadowCoord.z-.0005),shadowCoord.w));
	mat4 textureMat = mat4(
			1,0,0,0,
			0,-1,0,0,
			0,0,1,0,
			0,0,0,1
		);
		
	if(!reflection){
		finalColor = vec4( computeNormalLighting(lightNorm, normal, shadowFact, .35)*color, 1);
	}else{
		vec3 cubeCoord = (textureMat*vec4(reflect(view, normal),1)).xyz;
		vec4 diffuse = texture(refMap, cubeCoord);
		vec3 lRef = reflect(lightNorm, normal);
		
		if(dot(lightDir, -lightNorm) > cutOff){
			float distance = length(lightVec);
			float attenuation = 1.0/(1+atten*(distance*distance));
			
			float spec = specIntensity*pow(dot(lRef, view), specPower);
			
			vec3 finalDiff = max(dot(normal, lightNorm), .35)*diffuse.xyz;
			finalColor = vec4( max(shadowFact*attenuation*finalDiff+spec, .35*diffuse.xyz),1);
		}else{
			finalColor = vec4( .35*diffuse.xyz,1);
		}
	}
	
	/*if(matId == 0){
		finalColor = vec4(max((on ? dot(normal, lightNorm) : .2), .2)*color ,1);
	}else{
		vec4 diffuse = texture(image, vec3(uvs, matId-1));
		float diffuseFactor = max((on ? dot(normal, lightNorm) : .2), .2);
		//finalColor = diffuseFactor*diffuse;
		finalColor = diffuse;
	}*/
}