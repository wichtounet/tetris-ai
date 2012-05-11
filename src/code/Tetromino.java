package code;

import java.awt.Color;

/*Object representation of a tetromino.*/
public class Tetromino implements Cloneable
{
	/*Constructor.*/
	public Tetromino(){}
	
	
	/*Contents (Block array)*/
	public Block[][] array;
	
	
	/*Position, rotation, type, etc*/
	public volatile int x, y, rot, type;
	
	
	/*Color.*/
	public volatile Color color;
	
	
	/*Copy.*/
	public Tetromino clone()
	{
		Tetromino ret = new Tetromino();
		ret.array = array.clone();
		ret.x = x;
		ret.y = y;
		ret.rot = rot;
		ret.type = type;
		ret.color = color;
		return ret;
	}
	
	
	/*String representation.*/
	public String toString()
	{
		switch(type)
		{
		case 0:
			return "Long";
		case 1:
			return "Box";
		case 2:
			return "L";
		case 3:
			return "J";
		case 4:
			return "Dick";
		case 5:
			return "S";
		case 6:
			return "Z";
		default:
			return "NULL";
		}
	}
}
