
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import glMath.*;
import primitives.*;
import renderers.FBO;
import renderers.Reflector;
import renderers.Texture;
import renderers.InstanceRenderer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import core.*;
import collision.AABB;
import collision.ContactPair;
import collision.ContactRegister;
import ModelLoaders.SMDModel;

import java.util.ArrayList;
import java.util.HashMap;

public class CollisionTest {

	private float prevX, prevY;
	private Camera view;
	private PointLight light;
	private SpotLight spotLight;
	private HashMap<String, ShaderProgram> shaders;
	private Sphere sphere1;
	private Cylinder testCyl;
	private Cuboid cube;
//	private Torus testTorus;
	private Cone cone;
	//private InstanceRenderer instancer;
	private boolean stop;
	private int width, height, halfWidth, halfHeight;
	private SMDModel keyboard;
	private FBO shadowBuffer, refBuffer;
	private Reflector reflection;
	private GameObject reflectObj, normalObj, surface1;
	private ArrayList<GameObject> gameItems;
	private ContactRegister contacts;
	
	public CollisionTest(int width, int height){
		gameItems = new ArrayList<GameObject>();
		this.width = width;
		this.height = height;
		halfWidth = width/2;
		halfHeight = height/2;
		prevX = halfWidth;
		prevY = halfHeight;
		stop = false;
		contacts = new ContactRegister();

		shaders = new HashMap<String, ShaderProgram>();
		
		Mat4 perspective = MatrixUtil.getPerspective(45, width/(float)height, .01f, 30000);
		
		view = new Camera(0,50,800, 0,0);
		
//		view = new Camera(-350,50,350,-60,-10);
		
		sphere1 = new Sphere(50, 40, 0, false);
		
		cone = new Cone(50, 50, 40, 0, false);
		testCyl = new Cylinder(50,50,40,0,false);
		
		surface1 = new GameObject( new Plane(4096, 0,false),0,-100,0, .5f, .7f);
//		surface1 = new GameObject( new Cube(4096, 100, 4096, false,0,false),0,-100,0);
		
//		surface1.orient(0, 0, 1, 10);
		
		reflectObj = new GameObject(sphere1, -200, 0, 0, 0, 0, 0,
				0, 10, -.5f, .5f, .7f, true);
		
//		reflectObj.orient(0, 0, 1, 45);
		
		normalObj = new GameObject(testCyl, 500, 100, 0, 0,
				0, 0, 0, 10, -.5f, .5f, .7f, false);
		
		normalObj.orient(0, 0, 1, 60);
		
		gameItems.add(surface1);
		gameItems.add(normalObj);
		
		cube = new Cuboid(50,false,0,false);
//		cube = new Cuboid(100,20,40,false,0,false);
		
		int maxRows = 1;
//		int maxRows = 4;
//		int maxRows = 10;
		//use this for rotating
		for(int row = 0; row < maxRows; row++){
			 for(int box = 0; box < maxRows-row; box++){
				gameItems.add( new GameObject(cube, -row*30, row*75+25, box*55-((maxRows-row)/2.0f)*50+25, 
						 0,0,0,  
						 0, 10, -.5f, 
						 .5f, .7f, false));
			}
		}
		
		//use this for stacking
//		for(int row = 0; row < maxRows; row++){
//			 for(int box = 0; box < maxRows-row; box++){
//				gameItems.add( new GameObject(cube, 0, row*75+25, box*55-((maxRows-row)/2.0f)*50+25, 
//						 0,0,0,  
//						 0, 10, -.5f, 
//						 .5f, .7f, false));
//			}
//		}
		
		//use this for oblong shaped cubes
//		for(int row = 0; row < maxRows; row++){
//			 for(int box = 0; box < maxRows-row; box++){
//				gameItems.add( new GameObject(cube, row*50, row*40+10, box*45-((maxRows-row)/2.0f)*45+20, 
//						 0,0,0,  
//						 0, 10, -.5f, 
//						 .5f, .7f, false));
//			}
//		}
		
//		cube.orient(0, 0, 1, 60);
		
		/*keyboard = new SMDModel("models/keyboard.smd", false, "models/mats.txt");
		keyboard.scale(50);*/
		
		//light = new PointLight(0,500,0, 1, 1, 1, 25, 200, 1);
		spotLight = new SpotLight(0,500,0, 0,-1,0, 1,1,1, 120, 700, 1, 0);//atten .000005f
		
		spotLight.genShadowMap();
		
		shadowBuffer = FBO.create(spotLight.getShadowMap(), null, false);

		reflection = new Reflector(30000,-200,500,0);
		refBuffer = new FBO();
		reflection.bindWrite(refBuffer);
		refBuffer.setDrawBuffers(GL_COLOR_ATTACHMENT0);
		
		shaders.put("main", new ShaderProgram(
				new String[]{"shaders/main/vert.glsl", "shaders/main/frag.glsl"},
				new int[]{GL_VERTEX_SHADER, GL_FRAGMENT_SHADER}
				));
		
		shaders.put("shadow", new ShaderProgram(
				new String[]{"shaders/shadow/shadowVert.glsl", "shaders/shadow/shadowFrag.glsl"},
				new int[]{GL_VERTEX_SHADER, GL_FRAGMENT_SHADER}
				));
		
		shaders.put("reflectObj", new ShaderProgram(
				new String[]{"shaders/reflection/reflectVert.glsl", "shaders/reflection/reflectGeo.glsl", "shaders/reflection/reflectFrag.glsl"},
				new int[]{GL_VERTEX_SHADER, GL_GEOMETRY_SHADER, GL_FRAGMENT_SHADER}
				));
		
		ShaderProgram curShader = shaders.get("main");
		curShader.bind();
		
		curShader.setUniform("proj", perspective.asBuffer());
		curShader.setUniform("lightPos", spotLight.getPosBuffer());
		curShader.setUniform("image", 0);
		curShader.setUniform("refMap", 1);
		
		curShader.setUniform("cutOff", spotLight.getCutOff());
		curShader.setUniform("atten", spotLight.getAttenuation());
		curShader.setUniform("intensity", spotLight.getIntensity());
		
		curShader.setUniform("specIntensity", 1f);
		curShader.setUniform("specPower", 50f);
		
		curShader.setUniform("lightDir", spotLight.getDirBuffer());
		curShader.setUniform("lightView", spotLight.getViewBuffer());
		curShader.setUniform("lightProj", spotLight.getPerspectiveBuffer());
		
		curShader = shaders.get("reflectObj");
		curShader.bind();
		
		//reason to have a uniform buffer object
		curShader.setUniform("projection", reflection.getPerspectiveBuffer());
		curShader.setUniform("lightPos", spotLight.getPosBuffer());
		curShader.setUniform("image", 0);
		
		curShader.setUniform("cutOff", spotLight.getCutOff());
		curShader.setUniform("atten", spotLight.getAttenuation());
		curShader.setUniform("intensity", spotLight.getIntensity());
		
		curShader.setUniform("lightDir", spotLight.getDirBuffer());
		curShader.setUniform("lightView", spotLight.getViewBuffer());
		curShader.setUniform("lightProj", spotLight.getPerspectiveBuffer());
		curShader.setUniform("cams[0]", reflection.getCamBuffer());
		
		curShader = shaders.get("shadow");
		curShader.bind();
		
		curShader.setUniform("proj", spotLight.getPerspectiveBuffer());
		curShader.setUniform("view", spotLight.getViewBuffer());
		
		curShader.unbind();
		
		glClearColor(.5f, .6f, .8f, 1);
		glEnable(GL_DEPTH_TEST);
//		glEnable(GL_CULL_FACE);
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		
//		glBlendFunc(GL_SRC_COLOR, GL_SRC_COLOR);
//		glBlendEquation(GL_FUNC_ADD);
//		glEnable(GL_BLEND);
		//System.err.println(Util.translateGLErrorString(glGetError()));
	}	
	
	public void renderShadow(){
		shadowBuffer.bind();
		glViewport(0,0,1024, 1024);
		glClear(GL_DEPTH_BUFFER_BIT);
		ShaderProgram shader = shaders.get("shadow");
		shader.bind();
		
		reflectObj.render(shader, "model");
		
		for(GameObject item : gameItems){
			item.render(shader, "model");
		}
		
		shader.unbind();
		shadowBuffer.unbind();
	}
	
	public void renderReflection(){
		refBuffer.bind();
		glViewport(0,0,1024, 1024);
		glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
		ShaderProgram shader = shaders.get("reflectObj");
		
		shader.bind();
		spotLight.bindShadow(GL_TEXTURE0);
		
		shader.setUniform("cams[0]", reflection.getCamBuffer());
		shader.setUniform("view", view.getLookAt().asBuffer());
		
//		shader.setUniform("color", .1f, .4f, .8f);
//		surface1.render(shader, "model");
//		
//		shader.setUniform("color", .3f, .2f, .6f);
//		normalObj.render(shader, "model", "nMatrix");
		
		for(int curItem = 0; curItem < gameItems.size(); curItem++){
			shader.setUniform("color", .3f, (curItem/(float)gameItems.size())*.4f, curItem/(float)gameItems.size());
			gameItems.get(curItem).render(shader, "model", "nMatrix");
		}
		
		shader.unbind();
		refBuffer.unbind();
	}
	
	public void render(){
		renderShadow();
		renderReflection();
		glViewport(0,0,width, height);
		glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
		ShaderProgram shader = shaders.get("main");
		
		shader.bind();
		shader.setUniform("view", view.getLookAt().asBuffer());
		shader.setUniform("eye", view.getEye().asBuffer());
		
		spotLight.bindShadow(GL_TEXTURE0);
		reflection.bindRead(GL_TEXTURE1);
		
//		shader.setUniform("color", .3f, .2f, .6f);
//		normalObj.render(shader, "model", "nMatrix");
		
//		shader.setUniform("color", .5f, .2f, .6f);
		shader.setUniform("reflection", true);
		reflectObj.render(shader, "model", "nMatrix");
		shader.setUniform("reflection", false);
		
//		shader.setUniform("color", .1f, .4f, .8f);
//		surface1.render(shader, "model");
		
		for(int curItem = 0; curItem < gameItems.size(); curItem++){
			shader.setUniform("color", .3f, (curItem/(float)gameItems.size())*.4f, curItem/(float)gameItems.size());
			gameItems.get(curItem).render(shader, "model", "nMatrix");
		}
		
		shader.unbind();
	}
	
	public void transform(){

//		System.out.println("updating");
		normalObj.checkCollision(surface1);
		reflectObj.checkCollision(surface1);
		for(int current = 2; current < gameItems.size(); current++){
			GameObject cur = gameItems.get(current);
			cur.checkCollision(surface1);
		}
		
		for(int current = 2; current < gameItems.size(); current++){
			GameObject cur = gameItems.get(current);
			cur.checkCollision(reflectObj);
			cur.checkCollision(normalObj);
//			System.out.println("done updating");
			for(int curCheck = current+1; curCheck < gameItems.size(); curCheck++){
				cur.checkCollision(gameItems.get(curCheck));
			}
		}
		
//		for(int current = 0; current < gameItems.size(); current++){
//			GameObject cur = gameItems.get(current);
//			cur.checkCollision(reflectObj);
////			System.out.println("done updating");
//			for(int curCheck = current+1; curCheck < gameItems.size(); curCheck++){
//				cur.checkCollision(gameItems.get(curCheck));
//			}
//		}

//		for(int current = 0; current < gameItems.size(); current++){
//			GameObject cur = gameItems.get(current);
//			ContactPair contact = cur.checkCollision(reflectObj);
//			if(contact != null){
//				contacts.add(contact);
//			}
////			System.out.println("done updating");
//			for(int curCheck = current+1; curCheck < gameItems.size(); curCheck++){
//				contact = cur.checkCollision(gameItems.get(curCheck));
//				if(contact != null){
//					contacts.add(contact);
//				}
//			}
//		}
//		contacts.resolve(1);
//		contacts.clear();
		
		reflectObj.update();
		for(GameObject item : gameItems){
			item.update();
		}
		reflection.setPos(reflectObj);
	}
	
	public void loopControl(){
		keyboardLogic();
		mouseLogic();
		transform();
		render();
	}
	
	protected void keyboardLogic(){
		while(Keyboard.next()){
			if(Keyboard.getEventKey() == Keyboard.KEY_I){
				if(Keyboard.getEventKeyState()){
					transform();
				}
			}else if(Keyboard.getEventKey() == Keyboard.KEY_LEFT){
				if(Keyboard.getEventKeyState()){
//					surface.rotate(0,0,1,-1);
				}
			}else if(Keyboard.getEventKey() == Keyboard.KEY_RIGHT){
				if(Keyboard.getEventKeyState()){
					
				}
			}
			else if(Keyboard.getEventKey() == Keyboard.KEY_Q){
				if(Keyboard.getEventKeyState()){
					
				}
			}
		}
		
		if(Keyboard.isKeyDown(Keyboard.KEY_D)){
			view.strafe(10);
		}
		else if(Keyboard.isKeyDown(Keyboard.KEY_W)){
			view.fly(-10);
		}
		else if(Keyboard.isKeyDown(Keyboard.KEY_A)){
			view.strafe(-10);
		}
		else if(Keyboard.isKeyDown(Keyboard.KEY_S)){
			view.fly(10);
		}
		else if(Keyboard.isKeyDown(Keyboard.KEY_UP)){
			normalObj.accelerate(1, 0, 0);
		}
		else if(Keyboard.isKeyDown(Keyboard.KEY_DOWN)){
			normalObj.accelerate(-1, 0, 0);
		}else if(Keyboard.isKeyDown(Keyboard.KEY_Q)){
			for(GameObject item : gameItems){
				item.reset();
			}
			reflectObj.reset();
		}else if(Keyboard.isKeyDown(Keyboard.KEY_T)){
			transform();
		}
		
	}
	
	protected void mouseLogic(){
		//fail safe for if the mouse listener reacts before the camera is instantiated
//		if(view != null) {
//			if(!stop){
//				//move the view based on the current mouse and the previous mouse positions
//				//float dx = Mouse.getX()-prevX;
//				float dx = halfWidth-Mouse.getX();
//				float dy = Mouse.getY()-halfHeight;
////				System.out.println(dx+" "+dy);
//				//float dy = prevY-Mouse.getY();
//				view.rotate(dx);
//				view.lookY(dy, true);
//				Mouse.setCursorPosition(halfWidth, halfHeight);
//			}
//		}
//		while(Mouse.next()){
//			if(Mouse.getEventButton() == 0){
//				if(Mouse.getEventButtonState()){
//					stop = !stop;
//				}
//			}
//		}
		//fail safe for if the mouse listener reacts before the camera is instantiated
		if(view != null && Mouse.isInsideWindow()) {
			if(!stop){
				//move the view based on the current mouse and the previous mouse positions
				//float dx = Mouse.getX()-prevX;
				float dx = prevX-Mouse.getX();
				float dy = Mouse.getY()-prevY;
				//float dy = prevY-Mouse.getY();
				view.rotate(dx*.5f);
				view.lookY(dy*.5f, true);
				prevX = Mouse.getX();
				prevY = Mouse.getY();
			}else{
				prevX = Mouse.getX();
				prevY = Mouse.getY();
			}
		}
		else{
			if(!stop){
				prevX = halfWidth;
				prevY = halfHeight;
				Mouse.setCursorPosition((int)prevX, (int)prevY);
			}
		}
		while(Mouse.next()){
			if(Mouse.getEventButton() == 0){
				if(Mouse.getEventButtonState()){
					stop = !stop;
				}
			}
		}
	}
	
	public void clean(){
		surface1.delete();
		normalObj.delete();
		reflectObj.delete();
		cone.delete();
		cube.delete();
		for(GameObject item : gameItems){
			item.delete();
		}
		
		shaders.get("main").delete();
		shaders.get("shadow").delete();
		shaders.get("reflectObj").delete();
		
		reflection.delete();
		//light.cleanUp();
		spotLight.cleanUp();
		//keyboard.delete();
		shadowBuffer.delete();
		refBuffer.delete();
	}
}
