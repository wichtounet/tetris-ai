package code;

import static code.ProjectConstants.sleep_;

public abstract class AbstractAI {
    protected TetrisPanel panel;
    protected TetrisEngine engine;
    
    AIThread thread;
    
    volatile boolean flag = false;
    
    /*
     * Time (ms) AbstractAI has to wait per keypress.
     * (for maximum speed without crashing, set waittime = 1, do_drop on)
     */
    public static final int waittime = 1; //1 does crash...
    
    public AbstractAI(TetrisPanel panel) {
        this.panel = panel;
        
        engine = panel.engine;
        thread = new AIThread();
    }

    public void send_ready() {
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
            sleep_(TetrisAI.waittime);
            if (prev_state == engine.activeblock.rot || init_state == engine.activeblock.rot) {
                engine.keyslam();
                sleep_(TetrisAI.waittime > 3 ? TetrisAI.waittime : 3);
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
            sleep_(TetrisAI.waittime);
            if (prev_state == engine.activeblock.x) {
                engine.keyslam();
                sleep_(TetrisAI.waittime > 3 ? TetrisAI.waittime : 3);
            }
            prev_state = engine.activeblock.x;
        }
        if (flag && TetrisAI.do_drop) {
            engine.keyslam();
            // make the minimum 3 to fix a weird threading glitch
            sleep_(TetrisAI.waittime > 3 ? TetrisAI.waittime : 3);
            return;
        }
        while (flag && engine.blocksdropped == st_blocksdropped) {
            //Now move it down until it drops a new block.
            engine.keydown();
            sleep_(TetrisAI.waittime);
        }
    }
    
    
}