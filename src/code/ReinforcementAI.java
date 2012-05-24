package code;

import static code.ProjectConstants.sleep_;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
    
    //By default, the value of unknown state is the max reward
    private static final float DEFAULT_VALUE = 4 * REWARD_LESS_LEVEL;
    
    //Random number generator
    private static final Random random = new Random();
    
    private static HashMap<StateAction, Float> Q = new HashMap<StateAction, Float>();
        
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
        
        setThread(new AIThread());
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
        //Compute the current state
        State state = computeState(ge.blocks);
        
        //Get the best known action for this state
        BlockPosition action = getAction(ge, state);
        
        //Warning: Top and Max level are not the same
        int maxLevel = getMaxLevel(ge.blocks);
        
        //Do the action
        movehere(action.bx, action.rot);
        
        float reward = 0;
        
        //Reward difference in level
        int diffLevel = getMaxLevel(ge.blocks) - maxLevel;
        if(diffLevel < 0){
            reward += -diffLevel * REWARD_LESS_LEVEL;
        } else if(diffLevel == 0){
            reward += REWARD_SAME_LEVEL;
        } else {
            reward += diffLevel * REWARD_MORE_LEVEL;
        }
        
        //TODO HOLES
        //TODO EDGES
        
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