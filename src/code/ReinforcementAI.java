package code;

import static code.ProjectConstants.sleep_;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This the reinforcement learning AbstractAI. 
 * 
 * TODO: Nothing is done so far, only a random player
 */
public class ReinforcementAI extends AbstractAI {
    //General informations
    private static int iteration = -1;
    private static int totalScores = 0;
    private static int maxScore = -1;
    
    //Percentage of time where the agent explores another way
    private static final int epsilon = 7;
    
    //Learning rate
    private static final float alpha = 0.8f;
    
    //Discount factor
    private static final float lambda = 0.8f;
    
    private static final int LEVELS = 3;//WARNING Memory usage is exponential to LEVELS
    
    private static final float REWARD_LESS_LEVEL = 2.5f;
    private static final float REWARD_SAME_LEVEL = 1.0f;
    private static final float REWARD_MORE_LEVEL = -4.0f;
    
    private static final float REWARD_TOUCHING_EDGES = 0.3f;
    private static final float REWARD_TOUCHING_WALLS = 0.7f;
    private static final float REWARD_TOUCHING_FLOOR = 0.1f;
    
    private static final float REWARD_HOLES = -0.1f;
    private static final float REWARD_BLOCKADES = -0.1f;
    
    //By default, the value of unknown state is the max reward
    private static final float DEFAULT_VALUE = 4 * REWARD_LESS_LEVEL + 1.0f;
    
    //Random number generator
    private static final Random random = new Random();
    
    private static HashMap<StateAction, Float> Q = new HashMap<StateAction, Float>();
        
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
        
        setThread(new AIThread());
    }

    private void applyAction(BlockPosition action, byte[][] mockGrid) {
        byte[][] definition = TetrisEngine.blockdef[engine.activeblock.type][action.rot];

        int h;
        for (h = engine.height - 1;; h--) {

            // indicator. 1: fits. 0: doesn't fit. -1: game over.
            int fit_state = 1;

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (definition[j][i] >= 1) {
                        if (h + j >= engine.height) {
                            fit_state = 0;
                        } else if (h + j < 0) {
                            fit_state = -1;
                        } else {
                            if (mockGrid[i + action.bx][h + j] >= 1) {
                                fit_state = 0;
                            }

                            if (fit_state == 1) {
                                for (int h1 = h + j - 1; h1 >= 0; h1--) {
                                    if (mockGrid[i + action.bx][h1] >= 1) {
                                        fit_state = 0;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //We don't want game over so here:
            if (fit_state == -1) {
                return;
            }

            //1 = found!
            if (fit_state == 1) {
                break;
            }
        }

        // copy over block position
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (definition[j][i] == 1) {
                    mockGrid[action.bx + i][h + j] = 2;
                }
            }
        }

        // check for clears
        boolean foundline;
        do {
            foundline = false;
            ML:
            for (int i = mockGrid[0].length - 1; i >= 0; i--) {
                for (int y = 0; y < mockGrid.length; y++) {
                    if (!(mockGrid[y][i] > 0)) {
                        continue ML;
                    }
                }

                // line i is full, clear it and copy
                foundline = true;
                for (int a = i; a > 0; a--) {
                    for (int y = 0; y < mockGrid.length; y++) {
                        mockGrid[y][a] = mockGrid[y][a - 1];
                    }
                }
                break ML;
            }
        } while (foundline);
    }
    
    class AIThread extends Thread {
        @Override
        public void run() {
            while (flag) {
                try {
                    //If it's merely paused, do nothing; if it's actually game over
                    //then break loop entirely.
                    if (engine.state == ProjectConstants.GameState.PLAYING) {
                        if (engine.activeblock != null) {
                            //System.out.println("doAction()");
                            doAction(engine);
                            //engine.step();
                        }
                    }
                    
                    //safety
                    sleep_(waittime);
                } catch (Exception e) {
                    //e.printStackTrace();
                    //System.out.print("Aborting and retrying...\n");
                    //return;
                }
            }
        }
    }
    
    @Override
    public void send_ready(int score) {
        if(iteration > -1){
            totalScores += score;
            
            if(score > maxScore){
                maxScore = score;
            }
            
            System.out.print(iteration + ";" + score);
            System.out.print(";" + (totalScores / (iteration + 1)));
            System.out.println(";" + maxScore);
        }
        
        //Pass to the next iteration
        ++iteration;
        
        super.send_ready(score);
    }
    
    @Override
    protected BlockPosition computeBestFit(TetrisEngine ge) {  
        //TODO Remove That
        return null;
    }
    
    private void doAction(TetrisEngine ge) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(ReinforcementAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Compute the current state
        State state = computeState(ge.blocks);
        
        //Get the best known action for this state
        BlockPosition action = getAction(ge, state);
        
        //Warning: Top and Max level are not the same
        int maxLevel = getMaxLevel(ge.blocks);
        
        byte[][] mockGrid = mockGrid(ge);
        
        applyAction(action, mockGrid);
        
        float reward = 0;
        
        //horizontal pairs
        for (int i = 0; i < ge.height; i++) {
            for (int j = 0; j < ge.width - 1; j++) {
                if (j == 0 && mockGrid[j][i] == 2) {
                    reward += REWARD_TOUCHING_WALLS;
                }
                if (j + 1 == ge.width - 1 && mockGrid[j + 1][i] == 2) {
                    reward += REWARD_TOUCHING_WALLS;
                }
                if (mockGrid[j][i] + mockGrid[j + 1][i] >= 3) {
                    reward += REWARD_TOUCHING_EDGES;
                }
            }
        }

        //vertical pairs
        for (int i = 0; i < ge.width; i++) {
            for (int j = 0; j < ge.height - 1; j++) {
                if (j + 1 == ge.height - 1 && mockGrid[i][j + 1] == 2) {
                    reward += REWARD_TOUCHING_FLOOR;
                }
                if (mockGrid[i][j] + mockGrid[i][j + 1] >= 3) {
                    reward += REWARD_TOUCHING_EDGES;
                }
            }
        }

        //Penalize holes. Also penalize blocks above holes.
        for (int i = 0; i < ge.width; i++) {
            // Part 1: Count how many holes (space beneath blocks)
            boolean f = false;
            int holes = 0;
            for (int j = 0; j < ge.height; j++) {
                if (mockGrid[i][j] > 0) {
                    f = true;
                }
                if (f && mockGrid[i][j] == 0) {
                    holes++;
                }
            }

            // Part 2: Count how many blockades (block above space)
            f = false;
            int blockades = 0;
            for (int j = ge.height - 1; j >= 0; j--) {
                if (mockGrid[i][j] == 0) {
                    f = true;
                }
                if (f && mockGrid[i][j] > 0) {
                    blockades++;
                }
            }

            reward += holes * REWARD_HOLES;
            reward += blockades * REWARD_BLOCKADES;
        }
        
        //Do the action
        movehere(action.bx, action.rot);
        
        //Reward difference in level
        int diffLevel = getMaxLevel(ge.blocks) - maxLevel;
        if(diffLevel < 0){
            reward += -diffLevel * REWARD_LESS_LEVEL;
        } else if(diffLevel == 0){
            reward += REWARD_SAME_LEVEL;
        } else {
            reward += diffLevel * REWARD_MORE_LEVEL;
        }
                
        //Compute the new state
        State nextState = computeState(ge.blocks);
        BlockPosition nextAction = getAction(ge, nextState);
        
        //Tuples used as key for the value function
        StateAction sa = new StateAction(state, action);
        StateAction nsa = new StateAction(nextState, nextAction);
        
        //Update the Q(s, a)
        Q.put(sa, value(sa) + alpha * (reward + lambda * value(nsa) - value(sa)));
    }

    private State computeState(Block[][] blocks) {
        int topLevel = getTopLevel(blocks);
                
        State state  = new State();
        state.bs = new BitSet(LEVELS * blocks.length);
        
        for(int column = 0; column < blocks.length; ++column){
            for(int i = 0; i < LEVELS; ++i){
                if(blocks[column][i + topLevel].getState() == Block.FILLED){
                    state.bs.set(i * blocks.length + column);
                }
            }
        }
        
        return state;
    }

    private int getTopLevel(Block[][] blocks) {
        int minLevel = Integer.MAX_VALUE;
        
        for(int i = 0; i < blocks.length; ++i){
            for(int j = 0; j < blocks[i].length; ++j){
                if(blocks[i][j].getState() == Block.FILLED){
                    if(j < minLevel){
                        minLevel = j;
                    }
                }
            }
        }
        
        return Math.min(minLevel, blocks[0].length - 1 - (LEVELS - 1));
    }

    private int getMaxLevel(Block[][] blocks) {
        int minLevel = Integer.MAX_VALUE;
        
        for(int i = 0; i < blocks.length; ++i){
            for(int j = 0; j < blocks[i].length; ++j){
                if(blocks[i][j].getState() == Block.FILLED){
                    if(j < minLevel){
                        minLevel = j;
                    }
                }
            }
        }
        
        return blocks[0].length - minLevel;
    }

    private BlockPosition getAction(TetrisEngine ge, State state) {
        //All the possible actions
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);
        
        BlockPosition action = null;
        
        if(random.nextInt(100) < epsilon){
            //Explore a random action
            action = posfits.get(random.nextInt(posfits.size()));
        } else {
            float maxValue = 0;
            
            for(BlockPosition a : posfits){
                StateAction sa = new StateAction(state, a);
                
                float value = value(sa);
                
                if(value > maxValue || action == null){
                    maxValue = value;
                    action = a;
                }
            }
        }
        
        return action;
    }
    
    private float value(StateAction sa){
        if(!Q.containsKey(sa)){
            Q.put(sa, DEFAULT_VALUE);
            
            if(Q.size() % 1000 == 0){
                System.out.println("Grow to " + Q.size());
            }
            
            return DEFAULT_VALUE;
        }
        
        return Q.get(sa);
    }
}