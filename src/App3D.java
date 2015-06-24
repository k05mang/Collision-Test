
import java.awt.Dimension;
import java.awt.Toolkit;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * Class for creating an OpenGL context to draw with
 * 
 * @author Kevin
 *
 */
public class App3D {

	private DisplayMode curDisplay;
	private boolean isFullscreen;
	private int width, height, screenWidth, screenHeight;
	
	/**
	 * Constructs the class that retains information about the display window for the opengl drawing canvas
	 * 
	 * @param width Width of the window to be created
	 * @param height Height of the window to be created
	 * @param fullScreen Determines if the window should be in full screen mode or not
	 */
	public App3D(int width, int height, boolean fullScreen){
		isFullscreen = fullScreen;
		//get the width and height of the display
		Toolkit screenTools = Toolkit.getDefaultToolkit();
		Dimension screen = screenTools.getScreenSize();
		screenWidth = (int) screen.getWidth();
		screenHeight = (int) screen.getHeight();
		
		//if not in full screen mode set the display to the default one of the desktop
		if(!fullScreen){
			if(width > screenWidth){
				this.width = screenWidth;
			}else{
				this.width = width;
			}
			
			if(height > screenHeight){
				this.height = screenHeight;
			}else{
				this.height = height;
			}
			curDisplay = new DisplayMode(this.width, this.height);
			//curDisplay = new DisplayMode(screenWidth-20, screenHeight-40);
		}
		else{
			/*get the list of applicable displays that will work for full screen mode and iterate through them
			once a suitable one has been found use it, otherwise use the desktop default */
			try {
				DisplayMode[] modes = Display.getAvailableDisplayModes();
				
				for(int m = 0; m < modes.length;m++){
					if(modes[m].isFullscreenCapable() && modes[m].getWidth() == screenWidth && modes[m].getHeight() == screenHeight){
						curDisplay = modes[m];
						this.width = screenWidth;
						this.height = screenHeight;
						break;
					}
				}
				//if there was not a suitable display mode to use for full screen mode then the default display mode will be used
				if(curDisplay == null){
					this.width = screenWidth;
					this.height = screenHeight;
					curDisplay = new DisplayMode(this.width, this.height);
					isFullscreen = false;
				}
				
			} catch (LWJGLException e) {
				//if an error occurs while attempting to find a display mode the default display mode will be used instead
				e.printStackTrace();
				System.err.println("Failed to get display modes, using default");
				this.width = screenWidth;
				this.height = screenHeight;
				curDisplay = new DisplayMode(this.width, this.height);
				isFullscreen = false;
			}
		}
	}
	
	/**
	 * Called to create the window and the graphics context
	 */
	public void instantiate(){
		
		try {
			Display.setDisplayMode(curDisplay);
			Display.setFullscreen(isFullscreen);
			Display.setResizable(true);
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			cleanUp();
			System.exit(-1);
		}
		CollisionTest collisionTest = new CollisionTest(width, height);
		while(!Display.isCloseRequested()){
			Display.sync(60);
			collisionTest.loopControl();
			Display.update();
		}
		collisionTest.clean();
		cleanUp();
	}
	
	public void cleanUp(){
		Display.destroy();
	}
	
	public static void main(String[] args){
//		App3D canvas = new App3D(1280,720,true);
		App3D canvas = new App3D(1280,720,false);
//		App3D canvas = new App3D(1000,700,false);
		canvas.instantiate();
	}
}
