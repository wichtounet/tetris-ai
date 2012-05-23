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
    private static final int epsilon = 5;
    
    //Learning rate
    private static final float alpha = 0.8f;
    
    //Discount factor
    private static final float lambda = 0.8f;
    
    private static final int LEVELS = 2;
    
    private static final float REWARD_LESS_LEVEL = 200;
    private static final float REWARD_SAME_LEVEL = 50;
    private static final float REWARD_MORE_LEVEL = -250;
    
    private static final float DEFAULT_VALUE = 0;
    
    //Random number generator
    private static final Random random = new Random();
    
    private static HashMap<StateAction, Float> Q = new HashMap<StateAction, Float>();
    
    static {
        //TODO move that constant out of here
        final int width = 8;
        
        for(long value = 0; value < Math.pow(2, LEVELS * width); ++value){
            BitSet bitset = new BitSet(LEVELS * width);
            
            for(int b = 0; b < LEVELS * width; ++b){
                bitset.set(b, ((value >> b) & 0x1) == 1);
            }
            
            for(byte bx = (byte) ((-width / 2) - 2); bx <= (byte) ((width / 2) + 2); ++bx){
                for(byte rot = 0; rot < 4; ++rot){
                    State state = new State();
                    state.bs = bitset;
                    
                    BlockPosition action = new BlockPosition();
                    action.bx = bx;
                    action.rot = rot;
                    
                    StateAction sa = new StateAction(state, action);
                                        
                    Q.put(sa, DEFAULT_VALUE);
                }
            }
        }
    }
    
    //Warning: The object is constructed for every game, data should be stored static
    
    //TODO Be aware of anomalies
    //TODO The algorithm will node some more information for rewards and punishments
    //IDEA 1 Test the whole state before and after a choice and verify if punishment or reward
    //IDEA 2 Reward based on the new score, perhaps too primitive...
    
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
        
        setThread(new AIThread());
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
    
    class AIThread extends Thread {
        @Override
        public void run() {
            while (flag) {
                try {
                    //If it's merely paused, do nothing; if it's actually game over
                    //then break loop entirely.
                    if (engine.state == ProjectConstants.GameState.PLAYING) {
                        if (engine.activeblock != null) {
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
            
            System.out.println("Generation: " + iteration + " scored " + score);
            System.out.println("Mean: " + (totalScores / (iteration + 1)));
            System.out.println("Max: " + maxScore);
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
        State state = computeState(ge.blocks);
        
        BlockPosition action = getAction(ge, state);
        
        int topLevel = getTopLevel(ge.blocks);
        
        System.out.println("top level is "+topLevel);
                
        movehere(action.bx, action.rot);
        
        float reward = 0;
        
        //Reward difference in level
        int diffLevel = getTopLevel(ge.blocks) - topLevel;
        if(diffLevel > 0){
            reward += diffLevel * REWARD_LESS_LEVEL;
        } else if(diffLevel == 0){
            reward += REWARD_SAME_LEVEL;
        } else {
            reward += -diffLevel * REWARD_MORE_LEVEL;
        }
        
        System.out.println("From " + topLevel + " to " + getTopLevel(ge.blocks) + " reward = " + reward);
        
        //TODO HOLES
        //TODO EDGES
        
        State nextState = computeState(ge.blocks);
        BlockPosition nextAction = getAction(ge, nextState);
        
        //Tuples used as key for the value function
        StateAction sa = new StateAction(state, action);
        StateAction nsa = new StateAction(nextState, nextAction);
        
        //Update the Q(s, a)
        Q.put(sa, Q.get(sa) + alpha * (reward + lambda * Q.get(nsa) - Q.get(sa)));
        
        //System.out.println(Q.get(sa));
    }

    private State computeState(Block[][] blocks) {
        int topLevel = getTopLevel(blocks);
                
        State state  = new State();
        state.bs = new BitSet(LEVELS * blocks.length);
        
        for(int i = 0; i + topLevel < LEVELS; ++i){
            for(int j = 0; j < blocks[i+topLevel].length; ++j){
                if(blocks[i+topLevel][j].getState() == 0){
                    state.bs.clear(i * blocks.length + j);
                } else {
                    state.bs.set(i * blocks.length + j);
                }
            }            
        }
        
        return state;
    }

    private BlockPosition getAction(TetrisEngine ge, State state) {
        //All the possible actions
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);
        
        BlockPosition action = null;
        
        if(random.nextInt(100) < epsilon){
            //Explore a random action
            action = posfits.get(random.nextInt(posfits.size()));
            
            if(action == null){
                System.out.println("Exploration action is null");
            }
        } else {
            float maxValue = -1000000f;
            
            for(BlockPosition a : posfits){
                StateAction sa = new StateAction(state, a);
                                
                /*if(!Q.containsKey(sa)){
                    System.out.println("sa="+sa);
                }*/
                
                float value = Q.get(sa);
                
                if(value > maxValue){
                    maxValue = value;
                    action = a;
                }
            }
            
            System.out.println("Choose action with value = " + maxValue);
            
            if(action == null){
                System.out.println("Best action is null");
            }
        }
        
        return action;
    }
}