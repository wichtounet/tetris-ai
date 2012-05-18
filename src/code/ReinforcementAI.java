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
    
    public ReinforcementAI(TetrisPanel panel) {
        super(panel);
    }
    
    @Override
    protected BlockPosition computeBestFit(TetrisEngine ge) {
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);

        // Return final position.
        return posfits.get(new Random().nextInt(posfits.size()));
    }
}