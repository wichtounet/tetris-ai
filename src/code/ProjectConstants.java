package code;

import java.io.*;
import java.net.URL;

import javax.swing.JOptionPane;

/*This class contains a group of project constants.
 * Use import static tetris.code.ProjectConstants.*/
public class ProjectConstants
{
	
	/*Should the application start fullscreened?*/
	public static final boolean STARTFS = false;
	
	/*Yes this adds leading zeroes.*/
	public static String addLeadingZeroes(int n, int zeroes)
	{
		if(zeroes > 10)throw new IllegalArgumentException();
		String ret = Integer.toString(n);
		while(ret.length() < zeroes)
		{
			ret = "0" + ret;
		}
		return ret;
	}
	
	/*Sleeps the current thread.*/
	public static void sleep_(int n){
		try{
			Thread.sleep(n);
		}
		catch(Throwable t){
			// Might throw a ThreadDeath if we're sleeping while we terminate the thread
			// but we're just going to ignore it.
		}
	}
	
	
	/*Returns a resource as an InputStream. First it
	 * tries to create a FileInputStream from the parent
	 * directory (if contents are unzipped) and then tries
	 * to use getResourceAsStream if that fails.*/
	public static InputStream getResStream(String path)
	throws IOException
	{
		try{
			//This is actually helpful for those downloading it
			//with something other than Eclipse (Tortoise for example).
			//However this screws up with Eclipse.
			File f = new File("."+path);
			return new FileInputStream(f.getCanonicalFile());
		}catch(Exception ea)
		{
			//eclipse workaround.
			try{
			return ProjectConstants.class.getResourceAsStream(path);
			}catch(Exception ex){ex.printStackTrace();}
		}
		throw new RuntimeException("Filestream: " + path + " not found.");
		
	}
	
	/*Returns a resource as a URL object, for certain file
	 * parsing. Should accomodate Eclipse and other clients/IDEs
	 * as well. Currently it loads resources from Eclipse, the
	 * jar file, and from Tortoise.*/
	@SuppressWarnings("deprecation")
	public static URL getResURL(String path)
	throws IOException
	{
		try{
			File f = new File("."+path);
			if(!f.exists())throw new Exception();
			
			return f.getCanonicalFile().toURL();
		}catch(Exception e)
		{
			//eclipse workaround.
			try{
			return ProjectConstants.class.getResource(path);
			}catch(Exception ex){ex.printStackTrace();}
		}
		throw new RuntimeException("File: " + path + " not found.");
		
	}
	
	
	/*In case of errors, call this.*/
	public static String formatStackTrace(StackTraceElement[] e)
	{
		StringBuffer ret = new StringBuffer();
		for(StackTraceElement el: e)
		{
			ret.append("[");
			ret.append(el.getFileName()==null?
					"Unknown source":el.getFileName());
			ret.append(":");
			ret.append(el.getMethodName());
			ret.append(":");
			ret.append(el.getLineNumber());
			ret.append("]\n");
		}
		return ret.toString();
	}


	/*Enum representation of the current game's state*/
	//Moving this here lol.
	public enum GameState
	{
		STARTSCREEN,
		PLAYING,
		PAUSED,
		HISCORE,
		GAMEOVER,
		BUSY;
	}
}
