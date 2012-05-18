package code;

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
    
    //Percentage of time where the agent explores another way
    private static final int epsilon = 5;
    
    //Learning rate
    private static final double alpha = 0.5;
    
    //Discount factor
    private static final double lambda = 0.8;
    
    //Random number generator
    private static final Random random = new Random();
    
    private static HashMap<StateAction, Double> Q = new HashMap<StateAction, Double>();
    
    //Warning: The object is constructed for every game, data should be stored static
    
    //TODO Be aware of anomalies
    //TODO The algorithm will node some more information for rewards and punishments
    //IDEA 1 Test the whole state before and after a choice and verify if punishment or reward
    //IDEA 2 Reward based on the new score, perhaps too primitive...
    
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
        
        //TODO INit the Q(s, a) value function optimistically (max value)
    }

    @Override
    public void send_ready(int score) {
        if(iteration > -1){
            totalScores += score;
            
            System.out.println("Generation: " + iteration + " scored " + score);
            System.out.println("Mean: " + (totalScores / (iteration + 1)));
        }
        
        //Pass to the next iteration
        ++iteration;
        
        super.send_ready(score);
    }
    
    @Override
    protected BlockPosition computeBestFit(TetrisEngine ge) {
        //All the possible actions
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);
        
        //TODO Compute the state of the game
        State state = null;
        
        BlockPosition action;
        
        if(random.nextInt(100) < epsilon){
            //Explore a random action
            action = posfits.get(random.nextInt(posfits.size()));
        } else {
            BlockPosition max = null;
            double maxValue = Double.MIN_VALUE;
            
            for(BlockPosition a : posfits){
                StateAction sa = new StateAction(state, a);
                
                double value = Q.get(sa);
                
                if(value > maxValue){
                    maxValue = value;
                    max = a;
                }
            }
            
            action = max;
        }
        
        //TODO Execute the selected action, compute the next state and reward
        State nextState = null;
        BlockPosition nextAction = null;
        int reward = 0;
        
        //Tuples used as key for the value function
        StateAction sa = new StateAction(state, action);
        StateAction nsa = new StateAction(nextState, nextAction);
        
        //Update the Q(s, a)
        Q.put(sa, Q.get(sa) + alpha * (reward + lambda * Q.get(nsa) - Q.get(sa)));
        
        //TEMPORARY Return a random action
        return posfits.get(new Random().nextInt(posfits.size()));
    }
}