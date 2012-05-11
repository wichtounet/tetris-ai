package code;

import static code.ProjectConstants.sleep_;
import static code.ProjectConstants.GameState;
import java.util.*;

/*This is the default tetris playing AI. It holds a reference to
 * the tetris engines so it can send key events when necessary
 * and it knows the current block*/
public class TetrisAI
{
	private TetrisPanel panel;
	private TetrisEngine engine;
	AIThread thread;
	volatile boolean flag = false;
	
	/*Time (ms) AI has to wait per keypress.*/
	public static final int waittime = 20;
	
	// (for maximum speed without crashing, set waittime = 1, do_drop on)
	/*Do we use hard drops?*/
	public static final boolean do_drop = false;
	
	
	// Constants (sort of) for score evaluation.
	double _TOUCHING_EDGES = 3.97;
	double _TOUCHING_WALLS = 6.52;
	double _TOUCHING_FLOOR = 0.65;
	double _HEIGHT = -3.78;
	double _HOLES = -2.31;
	double _BLOCKADE = -0.59;
	double _CLEAR = 1.6;
	
	
	public TetrisAI(TetrisPanel panel){
		this.panel = panel;
		engine = panel.engine;
		thread = new AIThread();
	}
	
	public void send_ready(){
		if(!flag){
			thread.start();
			flag = true;
			engine.lastnewblock = System.currentTimeMillis();
		}
	}
	
	class AIThread extends Thread{
		public void run(){
			
			while(flag){
				
				try{
					//If it's merely paused, do nothing; if it's actually game over
					//then break loop entirely.
					if(engine.state == GameState.PLAYING){
						if(engine.activeblock == null) continue;
						
						BlockPosition temp = computeBestFit(engine);
						if(engine.state == GameState.PLAYING){
							
							int elx = temp.bx;
							int erot = temp.rot;
							
							//Move it!
							movehere(elx, erot);
						}
					}
					//safety
					sleep_(waittime);
				}catch(Exception e){
					//System.out.print("Aborting and retrying...\n");
					//return;
				}
			}
			
		}
		
		/*Keypresses to move block to calculated position.*/
		private void movehere(int finx, int finrot){
			int st_blocksdropped = engine.blocksdropped;
			
			// we're going to make another failsafe here: if at any time we rotate it
			// or move it and it doesn't move then it's stuck and we give up.
			
			int init_state = engine.activeblock.rot;
			int prev_state = init_state;
			while(flag && engine.activeblock.rot != finrot){
				//Rotate first so we don't get stuck in the edges.
				engine.keyrotate();
				
				//Now wait.
				sleep_(waittime);
				
				if(prev_state == engine.activeblock.rot || init_state == engine.activeblock.rot){
					engine.keyslam();
					sleep_(waittime>3? waittime : 3);
				}
				prev_state = engine.activeblock.rot;
			}
			
			prev_state = engine.activeblock.x;
			while(flag && engine.activeblock.x != finx){
				//Now nudge the block.
				if(engine.activeblock.x < finx){
					engine.keyright();
				}
				else if(engine.activeblock.x > finx){
					engine.keyleft();
				}
				
				sleep_(waittime);
				
				if(prev_state == engine.activeblock.x){
					engine.keyslam();
					sleep_(waittime>3? waittime : 3);
				}
				prev_state = engine.activeblock.x;
			}
			
			if(flag && do_drop){
				engine.keyslam();
				// make the minimum 3 to fix a weird threading glitch
				sleep_(waittime>3? waittime : 3);
				return;
			}
			
			while(flag && engine.blocksdropped == st_blocksdropped){
				//Now move it down until it drops a new block.
				engine.keydown();
				sleep_(waittime);
			}
		}
	}

	// =============== Here be the AI code. ===============
	
	/*This can calculate the best possible fit for it, given the current
	 * state the blocks are in.*/
	BlockPosition computeBestFit(TetrisEngine ge){

		byte[][][] allrotations = TetrisEngine.blockdef[ge.activeblock.type];
		int nrots = allrotations.length;

		// List of all the possible fits.
		List<BlockPosition> posfits = new ArrayList<BlockPosition>();

		// Loop through the rotations.
		// Here we generate all of the unique valid fits, and evaluate
		// them later.
		for(int i=0; i<nrots; i++){
			byte[][] trotation = allrotations[i];
			int free = freeSpaces(trotation);
			int freeL = free / 10;
			int freeR = free % 10;
			int minX = 0 - freeL;
			int maxX = (ge.width-4) + freeR;
			// now loop through each position for a rotation.
			for(int j=minX; j<=maxX; j++){
				BlockPosition put = new BlockPosition();
				put.bx = j;
				put.rot = i;
				posfits.add(put);
			}
		}
		
		// Do everything again for the next block
		byte[][][] allrotations2 = TetrisEngine.blockdef[ge.nextblock.type];
		int nrots2 = allrotations2.length;
		List<BlockPosition> posfits2 = new ArrayList<BlockPosition>();
		for(int i=0; i<nrots2; i++){
			byte[][] trotation = allrotations2[i];
			int free = freeSpaces(trotation);
			int freeL = free / 10;
			int freeR = free % 10;
			int minX = 0 - freeL;
			int maxX = (ge.width-4) + freeR;
			for(int j=minX; j<=maxX; j++){
				BlockPosition put = new BlockPosition();
				put.bx = j;
				put.rot = i;
				posfits2.add(put);
			}
		}

		// now we begin the evaluation.
		// for each element in the list we have, calculate a score, and pick
		// the best.
		double[] scores = new double[posfits.size() * posfits2.size()];

		for(int i=0; i<posfits.size(); i++){
			for(int j=0; j<posfits2.size(); j++){
				scores[i*posfits2.size()+j] = evalPosition(ge, posfits.get(i), posfits2.get(j));
			}
		}

		//retrieve max.
		double max = Double.NEGATIVE_INFINITY;
		BlockPosition max_b = null;
		for(int i=0; i<scores.length; i++){
			if(scores[i] >= max){
				max_b = posfits.get(i/posfits2.size());
				max = scores[i];
			}
		}

		// Return final position.
		return max_b;
	}

	// Evaluate position not with one, but with two blocks.
	double evalPosition(TetrisEngine ge, BlockPosition p, BlockPosition q){

		// First thing: Simulate the drop. Do this on a mock grid.
		// copying it here may seem like a waste but clearing it
		// after each iteration is too much of a hassle.

		// This copies the grid.
		byte[][] mockgrid = new byte[ge.width][ge.height];
		for(int i=0; i<ge.width; i++)
			for(int j=0; j<ge.height; j++){
				byte s = (byte) ge.blocks[i][j].getState();
				if(s==2) s=0;
				mockgrid[i][j] = s;
			}
		
		int cleared = 0;
		for(int block=1; block<=2; block++){
			
			byte[][] bl;
			BlockPosition r;
			
			if(block==1) r=p;
			else r=q;
			
			if(block==1) bl = ge.blockdef[ge.activeblock.type][r.rot];
			else bl = ge.blockdef[ge.nextblock.type][r.rot];

			// Now we find the fitting height by starting from the bottom and
			// working upwards. If we're fitting a line-block on an empty
			// grid then the height would be height-1, and it can't be any
			// lower than that, so that's where we'll start.
	
			int h;
			for(h = ge.height-1;; h--){
	
				// indicator. 1: fits. 0: doesn't fit. -1: game over.
				int fit_state = 1;
	
				for(int i=0; i<4; i++)
					for(int j=0; j<4; j++){
						//check for bounds.
	
	
						boolean block_p = bl[j][i] >= 1;
	
						//we have to simulate lazy evaluation in order to avoid
						//out of bounds errors.
	
						if(block_p){
							//still have to check for overflow. X-overflow can't
							//happen at this stage but Y-overflow can.
	
							if(h+j >= ge.height)
								fit_state = 0;
	
							else if(h+j < 0)
								fit_state = -1;
	
							else{
								boolean board_p = mockgrid[i+r.bx][h+j] >= 1;
	
								// Already filled, doesn't fit.
								if(board_p)
									fit_state = 0;
	
								// Still the possibility that another block
								// might still be over it.
								if(fit_state==1){
									for(int h1=h+j-1; h1>=0; h1--)
										if(mockgrid[i+r.bx][h1]>=1){
											fit_state = 0;
											break;
										}
								}
							}
						}
					}
	
				//We don't want game over so here:
				if(fit_state==-1)
					return -99999999;
				
				//1 = found!
				if(fit_state==1)
					break;
	
			}
	
			// copy over block position
			for(int i=0; i<4; i++)
				for(int j=0; j<4; j++)
					if(bl[j][i]==1)
						mockgrid[r.bx+i][h+j] = 2;
			
			
			// check for clears
			boolean foundline;
			do{
				foundline = false;
				ML:
				for(int i = mockgrid[0].length-1;i>=0;i--)
				{
					for(int y = 0;y < mockgrid.length;y++)
					{
						if(!(mockgrid[y][i] > 0))
							continue ML;
					}
					
					// line i is full, clear it and copy
					cleared++;
					foundline = true;
					for(int a = i;a>0;a--)
					{
						for(int y = 0;y < mockgrid.length;y++)
						{
							mockgrid[y][a] = mockgrid[y][a-1];
						}
					}
					break ML;
				}
			}while(foundline);
		}

		// Now we evaluate the resulting position.

		// Part of the evaluation algorithm is to count the number of touching sides.
		// We do this by generating all pairs and seeing how many them are touching.
		// If they add up to 3, it means one of them is from the active block and the
		// other is a normal block (ie. they're touching).

		double score = 0.0;

		//horizontal pairs
		for(int i=0; i<ge.height; i++)
			for(int j=0; j<ge.width-1; j++){
				if(j==0 && mockgrid[j][i]==2) score += _TOUCHING_WALLS;
				if(j+1==ge.width-1 && mockgrid[j+1][i]==2) score += _TOUCHING_WALLS;
				if(mockgrid[j][i] + mockgrid[j+1][i] >= 3) score += _TOUCHING_EDGES;
			}

		//vertical pairs
		for(int i=0; i<ge.width; i++)
			for(int j=0; j<ge.height-1; j++){
				if(j+1==ge.height-1 && mockgrid[i][j+1]==2) score += _TOUCHING_FLOOR;
				if(mockgrid[i][j] + mockgrid[i][j+1] >= 3) score += _TOUCHING_EDGES;
			}

		// Penalize height.
		for(int i=0; i<ge.width; i++)
			for(int j=0; j<ge.height; j++){
				int curheight = ge.height - j;
				if(mockgrid[i][j]>0) score += curheight * _HEIGHT;
			}

		//Penalize holes. Also penalize blocks above holes.
		for(int i=0; i<ge.width; i++) {
			
			// Part 1: Count how many holes (space beneath blocks)
			boolean f = false;
			int holes = 0;
			for(int j=0; j<ge.height; j++){
				if(mockgrid[i][j]>0) f = true;
				if(f && mockgrid[i][j]==0) holes++;
			}
			
			// Part 2: Count how many blockades (block above space)
			f = false;
			int blockades = 0;
			for(int j=ge.height-1; j>=0; j--){
				if(mockgrid[i][j]==0) f=true;
				if(f&&mockgrid[i][j]>0) blockades++;
			}
			
			score += _HOLES*holes;
			score += _BLOCKADE*blockades;
		}
		
		score += cleared * _CLEAR;
		
		/*for (int i1 = 0; i1 < mockgrid.length; i1++) {
			for (int i2 = 0; i2 < mockgrid[0].length; i2++) {
				System.out.print(mockgrid[i1][i2] + " ");
			}
			System.out.println();
		}
		System.out.println(score);*/
		//System.exit(0);

		return score;
	}


	// Takes a int array and calculates how many blocks of free spaces are there
	// on the left and right. The return value is a 2 digit integer.
	static int freeSpaces(byte[][] in){

		// It's free if all of them are zero, and their sum is zero.
		boolean c1free = in[0][0] + in[1][0] + in[2][0] + in[3][0] == 0;
		boolean c2free = in[0][1] + in[1][1] + in[2][1] + in[3][1] == 0;
		boolean c3free = in[0][2] + in[1][2] + in[2][2] + in[3][2] == 0;
		boolean c4free = in[0][3] + in[1][3] + in[2][3] + in[3][3] == 0;

		int lfree = 0;
		// Meh, I'm too lazy to code a loop for this.
		if(c1free){
			lfree++;
			if(c2free){
				lfree++;
				if(c3free){
					lfree++;
					if(c4free){
						lfree++;
		} } } }

		int rfree = 0;
		if(c4free){
			rfree++;
			if(c3free){
				rfree++;
				if(c2free){
					rfree++;
					if(c1free){
						rfree++;
		} } } }

		return lfree*10 + rfree;
	}
	
}

// No tuple support in java.
class BlockPosition{
	int bx, rot;
}
