package code;

import java.applet.*;
import java.io.*;

import javax.sound.midi.*;
import javax.swing.JOptionPane;

import static code.ProjectConstants.*;

/*This class loads, plays, and manages sound effects and
 * music for Tetris4j. The sound URL's are hardcoded
 * into this class and is loaded statically at runtime.*/
public class SoundManager
{
	
	/*This represents the list of sounds available.*/
	public static enum Sounds{
		// sound/tetris.midi
		TETRIS_THEME, 
		
		// sound/soundfall.wav
		FALL,
		
		// sound/soundrotate.wav
		ROTATE,
		
		// sound/soundclear.wav
		CLEAR, 
		
		// sound/soundtetris.wav
		TETRIS,
		
		// sound/sounddie.wav
		DIE;
	}
	
	// do we even play music at all?
	public static final boolean PLAY_MUSIC = true;
	
	private Sequencer midiseq; //Midi sequencer, plays the music.
	
	private InputStream tetheme; //Tetris theme (midi-inputstream).
	
		//The collection of
		//sound effects used.
	private AudioClip sx1, sx2, sx3, sx4, sx5; 
	
	private static SoundManager soundmanager = null;
		//Reference of the SoundManager.
	
	/*Since this class locks certain system resources, it's
	 * best to only have one instance of this class. If an
	 * instance of SoundManager already exists, this replaces
	 * that with a new instance.*/
	public static SoundManager getSoundManager()
	{
		soundmanager = new SoundManager();
		return soundmanager;
	}
	
	//private initializer method.
	private SoundManager(){
		try
		{
			tetheme = getResStream("/sound/tetris.midi");
			sx1 = loadsound("/sound/soundfall.wav");
			sx2 = loadsound("/sound/soundrotate.wav");
			sx3 = loadsound("/sound/soundtetris.wav");
			sx4 = loadsound("/sound/soundclear.wav");
			sx5 = loadsound("/sound/sounddie.wav");
		} catch (Exception e)
		{
			throw new RuntimeException("Cannot load sound.");
		} 
	}
	
	/*Plays a sound. Sounds should be short because once this
	 * is called again, the previous sound teminates and
	 * the new sound starts.*/
	public synchronized void sfx(Sounds s)
	{
		if(!PLAY_MUSIC) return;
		
		switch(s)
		{
		case FALL:
			sx1.play();
			break;
		case ROTATE:
			sx2.play();
			break;
		case TETRIS:
			sx3.play();
			break;
		case CLEAR:
			sx4.play();
			break;
		case DIE:
			sx5.play();
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	/*Plays a music track. Currently the only track
	 * is the default MIDI track (theme song).*/
	public synchronized void music(Sounds s)
	{
		if(!PLAY_MUSIC) return;

		if(s==null)
		{
			midiseq.close();
			return;
		}
		
		else if(s == Sounds.TETRIS_THEME)
		{
			
			try{
				midiseq = MidiSystem.getSequencer();
				midiseq = MidiSystem.getSequencer();
				midiseq.open();
				//Sometimes throws MidiUnavailableException.
				midiseq.setSequence(MidiSystem.getSequence(tetheme));
				midiseq.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
				midiseq.start();
			}catch(Exception e){
				throw new RuntimeException("Cannot play MIDI.");}
					
		}
		else throw new IllegalArgumentException();
	}
	
	//returns an AudioClip from a String filename.
	private static AudioClip loadsound(String name)
	throws IOException
	{
		if(new File(getResURL(name).getFile()).exists())
			return Applet.newAudioClip(getResURL(name));
		else throw new IOException("NOT FOUND: " + getResURL(name));
	}
}
