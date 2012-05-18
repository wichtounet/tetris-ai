package code;

import java.util.List;
import java.util.Random;

/*
 * This the reinforcement learning AbstractAI. 
 * 
 * TODO: Nothing is done so far, only a random player
 */
public class ReinforcementAI extends AbstractAI {    
    //TODO Store the action for each possible state
    //The action should be a BlockPosition object
    
    private static int iteration = -1;
    private static int totalScores = 0;
    
    //Warning: The object is constructed for every game, data should be stored static
    
    //TODO Be aware of anomalies
    //TODO The algorithm will node some more information for rewards and punishments
    //IDEA 1 Test the whole state before and after a choice and verify if punishment or reward
    //IDEA 2 Reward based on the new score, perhaps too primitive...
    
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
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
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);
        
        return posfits.get(new Random().nextInt(posfits.size()));
    }
}