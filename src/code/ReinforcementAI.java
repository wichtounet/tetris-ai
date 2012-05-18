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
    
    private static int iteration = 0;
    
    //Warning: The object is constructed for every game, data should be stored static
    
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
    }

    @Override
    public void send_ready() {
        super.send_ready();
        
        System.out.println("Generation: " + iteration++);
    }
    
    @Override
    protected BlockPosition computeBestFit(TetrisEngine ge) {
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);

        // Return final position.
        return posfits.get(new Random().nextInt(posfits.size()));
    }
}