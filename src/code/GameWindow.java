package code;

import static code.ProjectConstants.*;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.midi.*;
import javax.swing.*;

import code.SoundManager.Sounds;


/*The game window.*/
public class GameWindow extends JFrame
{
	
	private GraphicsDevice dev;
	private TetrisPanel t;
	
	
	/*Creates a GameWindow, by default.*/
	public GameWindow()
	{
		this(STARTFS, null);
	}
	
	
	/*Creates a GameWindow and make it fullscreen or not.
	 * May be from another GameWindow.*/
	public GameWindow(boolean fullscreen, GameWindow old)
	{
		super();
		if(fullscreen)
		{
			createFullscreenWindow(old);
		}
		
		else createWindow(old);
		
		if(old!=null)
		{
			//Cleanup
			old.setVisible(false);
			old.dispose();
			old = null;
		}
	}
	
	
	
	private void createWindow(GameWindow old)
	{
		setUndecorated(false);
		setTitle("JTetris");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		
		if(old != null)
			t = old.t;
		else t = new TetrisPanel();
		
		t.setPreferredSize(new Dimension(800,600));
		setContentPane(t);
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		if(old==null)
			t.engine.startengine();
	}
	
	
	
	private void createFullscreenWindow(GameWindow old)
	{
		setUndecorated(true);
		setTitle("JTetris");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		
		if(old != null)
			t = old.t;
		else t = new TetrisPanel();
		
		t.setPreferredSize(new Dimension(800,600));
		setContentPane(t);
		
		try{
			dev =  GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			dev.setFullScreenWindow(this);
			dev.setDisplayMode(new DisplayMode
					(800,600,32,DisplayMode.REFRESH_RATE_UNKNOWN));
		}catch(Throwable t){
				throw new RuntimeException("Getting screen device failed");
		}
		
		t.setPreferredSize(new Dimension(800,600));
		setContentPane(t);
		
		setVisible(true);
		SwingUtilities.updateComponentTreeUI(this);
		
		if(old==null)
			t.engine.startengine();
	}
	
	
	/*Creates a fullscreen window from an old window.*/
	public static GameWindow enterFullScreen(GameWindow win)
	{
		win = new GameWindow(true, win);
		try{
			win.dev.setFullScreenWindow(win);
			//800x600 fullscreen?
			win.dev.setDisplayMode(new DisplayMode
					(800,600,32,DisplayMode.REFRESH_RATE_UNKNOWN));
		
		}catch(Throwable t)
		{
			win.dev.setFullScreenWindow(null);
			throw new RuntimeException("Failed fullscreen");
		}
		return win;
	}
	
	
	/*Creates a windowed window (lol?) from an old window.*/
	public static GameWindow exitFullScreen(GameWindow win)
	{
		if(win.dev != null)
			win.dev.setFullScreenWindow(null);
		win = new GameWindow(false, win);
		return win;
	}
}
