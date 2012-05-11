package code;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import static code.ProjectConstants.*;

/*TetrisPanel is the panel that contains the (main)
 * panels AKA. core. This also holds most of the objects
 * needed to render the game on a JDesktopPane.*/
public class TetrisPanel extends JPanel
{
	
	//---------------BEGIN PUBLIC VARIABLES---------------//
	
	/*Public reference to the TetrisEngine object.*/
	public TetrisEngine engine;
	
	/*Reference to the static SoundManager object.*/
	public SoundManager sound;
	
	/*Background image used for the game.*/
	public Image bg = null;
	
	/*Foreground image.*/
	public Image fg = null;
	
	/*Is it being controlled by a human or ai?*/
	public boolean isHumanControlled = false;
	
	/*AI object controlling the game.*/
	public TetrisAI controller = null;
	
	/*Genetic algorithm to find AI combinations*/
	public GeneticAIFinder genetic = null;
	
	/*Public TetrisPanel constructor.*/
	public TetrisPanel()
	{
		//Initialize the TetrisEngine object.
		engine = new TetrisEngine(this);
		
		sound = SoundManager.getSoundManager();
		
		//This is the bg-image.
		try
		{
			bg = ImageIO.read
				(getResURL("/image/background.png"));
			fg = ImageIO.read
				(getResURL("/image/backlayer.png"));
			
			// Actually, the background is the actual background plus
			// the meta image.
			Image meta = ImageIO.read
				(getResURL("/image/metalayer.png"));
			Graphics g = bg.getGraphics();
			g.drawImage(meta, 0, 0, null);
			
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Cannot load image.");
		}
		
		//Animation loop. Updates every 40 milliseconds (25 fps).
		new Thread(){
			public void run()
			{
				while(true)
				{
					sleep_(40);
					repaint();
				}
			}
		}.start();

		//Add all these key functions.
		KeyPressManager kpm = new KeyPressManager();
		kpm.putKey(KeyEvent.VK_LEFT, new Runnable(){public void run(){TetrisPanel.this.engine.keyleft();}});
		kpm.putKey(KeyEvent.VK_RIGHT, new Runnable(){public void run(){TetrisPanel.this.engine.keyright();}});
		kpm.putKey(KeyEvent.VK_DOWN, new Runnable(){public void run(){TetrisPanel.this.engine.keydown();}});
		kpm.putKey(KeyEvent.VK_SPACE, new Runnable(){public void run(){TetrisPanel.this.engine.keyslam();}});
		kpm.putKey(KeyEvent.VK_UP, new Runnable(){public void run(){TetrisPanel.this.engine.keyrotate();}});
		kpm.putKey(KeyEvent.VK_Z, new Runnable(){public void run(){TetrisPanel.this.engine.keyrotate();}});
		kpm.putKey(KeyEvent.VK_SHIFT, new Runnable(){public void run(){
			if(engine.state != GameState.GAMEOVER && controller != null && !controller.thread.isAlive())
				controller.send_ready();
			if(engine.state==GameState.PAUSED)
				engine.state = GameState.PLAYING;
			else{
				engine.state = GameState.PAUSED;
				//System.out.println(controller.thread.isAlive());
			}
		}});
		
		addKeyListener(kpm);
		
		//Focus when clicked.
		addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent me)
			{
				TetrisPanel.this.requestFocusInWindow();
			}});
		
		setFocusable(true);
		engine.state = GameState.PAUSED;
		
		sound.music(SoundManager.Sounds.TETRIS_THEME);
		
		if(!isHumanControlled){
			controller = new TetrisAI(this);
			genetic = new GeneticAIFinder(engine);
			genetic.setAIValues(controller);
		}
	}
	
	/*Paints this component, called with repaint().*/
	public void paintComponent(Graphics g)
	{
		//Necessary mostly because this is a JDesktopPane and
		//not a JPanel.
		super.paintComponent(g);
		
		//Draw: background, then main, then foreground.
		g.drawImage(bg, 0, 0, this);
		engine.draw(g);
		g.drawImage(fg, 0, 0, this);
	
	}

	/*
	This is a class that manages key presses. It's so that each press is sent
	once, and if you hold a key, it doesn't count as multiple presses.

	Note that some keys should never be counted more than once.
	*/
	class KeyPressManager extends KeyAdapter{

		static final int delay = 40;

		class KeyHandlingThread extends Thread{
			volatile boolean flag = true;
			public void run(){
				// The key handling loop.
				// Each iteration, call the functions whose keys are currently
				// being held down.

				while(flag){
					sleep_(delay);

					//if(keys[0]) keymap.get(KeyEvent.VK_LEFT).run();
					//if(keys[1]) keymap.get(KeyEvent.VK_RIGHT).run();
					if(keys[2]) keymap.get(KeyEvent.VK_DOWN).run();
				}
			}
		}

		KeyHandlingThread keythread;

		// After some testing: I think that it's best to only have the down button
		// have special handling.
		// Lol now I think it's a bit of a waste to have an entire thread running
		// for one button that's barely used.

		// Only keys that require special handling:
		// keys[0]: left
		// keys[1]: right
		// keys[2]: down
		volatile boolean[] keys = {false,false,false};

		KeyPressManager(){
			keythread = new KeyHandlingThread();

			if(TetrisPanel.this.isHumanControlled)
				keythread.start();
		}

		void putKey(int i, Runnable r){
			keymap.put(i, r);
		}

		// This hashmap maps from an Int (from KeyEvent.getKeyCode()) to a
		// function, represented by a Runnable.
		Map<Integer,Runnable> keymap = new HashMap<Integer,Runnable>();

		// Called when keypress is detected.
		public void keyPressed(KeyEvent ke){

			// Make special adjustments for handling of the shift key.
			if(!TetrisPanel.this.isHumanControlled && ke.getKeyCode() != KeyEvent.VK_SHIFT) return;

			int ck = ke.getKeyCode();
			if(keymap.containsKey(ck)){
				/*
				if(ck==KeyEvent.VK_LEFT)
					keys[0]=true;
				else if(ck==KeyEvent.VK_RIGHT)
					keys[1]=true;
				*/

				if(ck==KeyEvent.VK_DOWN)
					keys[2]=true;
				
				else keymap.get(ck).run();
			}
		}


		// Called when key is released. Here we'll want to modify the map.
		public void keyReleased(KeyEvent ke){
			
			if(!TetrisPanel.this.isHumanControlled) return;

			int ck = ke.getKeyCode();

			/*
			if(ck==KeyEvent.VK_LEFT)
				keys[0]=false;
			else if(ck==KeyEvent.VK_RIGHT)
				keys[1]=false;
			*/


			if(ck==KeyEvent.VK_DOWN)
				keys[2]=false;
		}
	}


}
