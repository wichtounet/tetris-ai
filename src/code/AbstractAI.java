package code;

import static code.ProjectConstants.sleep_;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAI {
    protected TetrisPanel panel;
    protected TetrisEngine engine;
    
    Thread thread;
    
    volatile boolean flag = false;
    
    /*
     * Time (ms) AbstractAI has to wait per keypress.
     * (for maximum speed without crashing, set waittime = 1, do_drop on)
     */
    public static final int waittime = 20; //1 does crash...
    
    /*
     * Do we use hard drops?
     */
    public static final boolean do_drop = true;
    
    public AbstractAI(TetrisPanel panel) {
        this.panel = panel;
        
        engine = panel.engine;
    }
    
    public void setThread(Thread thread){
        this.thread = thread;
    }
    
    public void send_ready(int lastscore) {
        if (!flag) {
            thread.start();
            flag = true;
            engine.lastnewblock = System.currentTimeMillis();
        }
    }
    
    protected abstract BlockPosition computeBestFit(TetrisEngine engine);
    
    class AIThread extends Thread {
        @Override
        public void run() {
            while (flag) {
                try {
                    //If it's merely paused, do nothing; if it's actually game over
                    //then break loop entirely.
                    if (engine.state == ProjectConstants.GameState.PLAYING) {
                        if (engine.activeblock == null) {
                            continue;
                        }

                        BlockPosition temp = computeBestFit(engine);
                        if (engine.state == ProjectConstants.GameState.PLAYING) {
                            int elx = temp.bx;
                            int erot = temp.rot;

                            //Move it!
                            movehere(elx, erot);
                        }
                    }
                    //safety
                    sleep_(waittime);
                } catch (Exception e) {
                    //System.out.print("Aborting and retrying...\n");
                    //return;
                }
            }
        }
    }

    protected void movehere(int finx, int finrot) {
        int st_blocksdropped = engine.blocksdropped;
        // we're going to make another failsafe here: if at any time we rotate it
        // or move it and it doesn't move then it's stuck and we give up.
        int init_state = engine.activeblock.rot;
        int prev_state = init_state;
        while (flag && engine.activeblock.rot != finrot) {
            //Rotate first so we don't get stuck in the edges.
            engine.keyrotate();
            //Now wait.
            sleep_(waittime);
            if (prev_state == engine.activeblock.rot || init_state == engine.activeblock.rot) {
                engine.keyslam();
                sleep_(waittime > 3 ? waittime : 3);
            }
            prev_state = engine.activeblock.rot;
        }
        prev_state = engine.activeblock.x;
        while (flag && engine.activeblock.x != finx) {
            //Now nudge the block.
            if (engine.activeblock.x < finx) {
                engine.keyright();
            } else if (engine.activeblock.x > finx) {
                engine.keyleft();
            }
            sleep_(waittime);
            if (prev_state == engine.activeblock.x) {
                engine.keyslam();
                sleep_(waittime > 3 ? waittime : 3);
            }
            prev_state = engine.activeblock.x;
        }
        if (flag && do_drop) {
            engine.keyslam();
            // make the minimum 3 to fix a weird threading glitch
            sleep_(waittime > 3 ? waittime : 3);
            return;
        }
        while (flag && engine.blocksdropped == st_blocksdropped) {
            //Now move it down until it drops a new block.
            engine.keydown();
            sleep_(waittime);
        }
    }

    // Takes a int array and calculates how many blocks of free spaces are there
    // on the left and right. The return value is a 2 digit integer.
    static int freeSpaces(byte[][] in) {

        // It's free if all of them are zero, and their sum is zero.
        boolean c1free = in[0][0] + in[1][0] + in[2][0] + in[3][0] == 0;
        boolean c2free = in[0][1] + in[1][1] + in[2][1] + in[3][1] == 0;
        boolean c3free = in[0][2] + in[1][2] + in[2][2] + in[3][2] == 0;
        boolean c4free = in[0][3] + in[1][3] + in[2][3] + in[3][3] == 0;

        int lfree = 0;
        // Meh, I'm too lazy to code a loop for this.
        if (c1free) {
            lfree++;
            if (c2free) {
                lfree++;
                if (c3free) {
                    lfree++;
                    if (c4free) {
                        lfree++;
                    }
                }
            }
        }

        int rfree = 0;
        if (c4free) {
            rfree++;
            if (c3free) {
                rfree++;
                if (c2free) {
                    rfree++;
                    if (c1free) {
                        rfree++;
                    }
                }
            }
        }

        return lfree * 10 + rfree;
    }
    
    // List of all the possible fits.
    protected List<BlockPosition> getPossibleFits(code.TetrisEngine ge, int type) {
        byte[][][] rotations = TetrisEngine.blockdef[type];
        int nrots = rotations.length;
        
        List<BlockPosition> posfits = new ArrayList<BlockPosition>();
        
        for (int i = 0; i < nrots; i++) {
            byte[][] trotation = rotations[i];
            int free = freeSpaces(trotation);
            int freeL = free / 10;
            int freeR = free % 10;
            int minX = 0 - freeL;
            int maxX = (ge.width - 4) + freeR;
            // now loop through each position for a rotation.
            for (int j = minX; j <= maxX; j++) {
                BlockPosition put = new BlockPosition();
                put.bx = (byte) j;
                put.rot = (byte) i;
                posfits.add(put);
            }
        }
        
        return posfits;
    }
}