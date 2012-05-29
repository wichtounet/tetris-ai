package code;

import java.util.List;

/*
 * This is the default tetris playing AbstractAI. It holds a reference to the tetris
 * engines so it can send key events when necessary and it knows the current block
 */
public class TetrisAI extends AbstractAI {
    // Constants (sort of) for score evaluation.
    double _TOUCHING_EDGES = 3.97;
    double _TOUCHING_WALLS = 6.52;
    double _TOUCHING_FLOOR = 0.65;
    double _HEIGHT = -3.78;
    double _HOLES = -2.31;
    double _BLOCKADE = -0.59;
    double _CLEAR = 1.6;

    public TetrisAI(TetrisPanel panel) {
        super(panel);
        
        setThread(new AIThread());
    }
    
    @Override
    protected BlockPosition computeBestFit(TetrisEngine ge) {        
        List<BlockPosition> posfits = getPossibleFits(ge, ge.activeblock.type);
        List<BlockPosition> posfits2 = getPossibleFits(ge, ge.nextblock.type);

        // now we begin the evaluation.
        // for each element in the list we have, calculate a score, and pick
        // the best.
        double[] scores = new double[posfits.size() * posfits2.size()];

        for (int i = 0; i < posfits.size(); i++) {
            for (int j = 0; j < posfits2.size(); j++) {
                scores[i * posfits2.size() + j] = evalPosition(ge, posfits.get(i), posfits2.get(j));
            }
        }

        //retrieve max.
        double max = Double.NEGATIVE_INFINITY;
        BlockPosition max_b = null;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] >= max) {
                max_b = posfits.get(i / posfits2.size());
                max = scores[i];
            }
        }

        // Return final position.
        return max_b;
    }

    // Evaluate position not with one, but with two blocks.
    double evalPosition(TetrisEngine ge, BlockPosition p, BlockPosition q) {

        // First thing: Simulate the drop. Do this on a mock grid.
        // copying it here may seem like a waste but clearing it
        // after each iteration is too much of a hassle.

        // This copies the grid.
        byte[][] mockgrid = mockGrid(ge);

        int cleared = 0;
        for (int block = 1; block <= 2; block++) {

            byte[][] bl;
            BlockPosition r;

            if (block == 1) {
                r = p;
            } else {
                r = q;
            }

            if (block == 1) {
                bl = TetrisEngine.blockdef[ge.activeblock.type][r.rot];
            } else {
                bl = TetrisEngine.blockdef[ge.nextblock.type][r.rot];
            }

            // Now we find the fitting HEIGHT by starting from the bottom and
            // working upwards. If we're fitting a line-block on an empty
            // grid then the HEIGHT would be HEIGHT-1, and it can't be any
            // lower than that, so that's where we'll start.

            int h;
            for (h = TetrisEngine.HEIGHT - 1;; h--) {

                // indicator. 1: fits. 0: doesn't fit. -1: game over.
                int fit_state = 1;

                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        //check for bounds.


                        boolean block_p = bl[j][i] >= 1;

                        //we have to simulate lazy evaluation in order to avoid
                        //out of bounds errors.

                        if (block_p) {
                            //still have to check for overflow. X-overflow can't
                            //happen at this stage but Y-overflow can.

                            if (h + j >= TetrisEngine.HEIGHT) {
                                fit_state = 0;
                            } else if (h + j < 0) {
                                fit_state = -1;
                            } else {
                                boolean board_p = mockgrid[i + r.bx][h + j] >= 1;

                                // Already filled, doesn't fit.
                                if (board_p) {
                                    fit_state = 0;
                                }

                                // Still the possibility that another block
                                // might still be over it.
                                if (fit_state == 1) {
                                    for (int h1 = h + j - 1; h1 >= 0; h1--) {
                                        if (mockgrid[i + r.bx][h1] >= 1) {
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
                    return -99999999;
                }

                //1 = found!
                if (fit_state == 1) {
                    break;
                }

            }

            // copy over block position
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (bl[j][i] == 1) {
                        mockgrid[r.bx + i][h + j] = 2;
                    }
                }
            }


            // check for clears
            boolean foundline;
            do {
                foundline = false;
                ML:
                for (int i = mockgrid[0].length - 1; i >= 0; i--) {
                    for (int y = 0; y < mockgrid.length; y++) {
                        if (!(mockgrid[y][i] > 0)) {
                            continue ML;
                        }
                    }

                    // line i is full, clear it and copy
                    cleared++;
                    foundline = true;
                    for (int a = i; a > 0; a--) {
                        for (int y = 0; y < mockgrid.length; y++) {
                            mockgrid[y][a] = mockgrid[y][a - 1];
                        }
                    }
                    break ML;
                }
            } while (foundline);
        }

        // Now we evaluate the resulting position.

        // Part of the evaluation algorithm is to count the number of touching sides.
        // We do this by generating all pairs and seeing how many them are touching.
        // If they add up to 3, it means one of them is from the active block and the
        // other is a normal block (ie. they're touching).

        double score = 0.0;

        //horizontal pairs
        for (int i = 0; i < TetrisEngine.HEIGHT; i++) {
            for (int j = 0; j < TetrisEngine.WIDTH - 1; j++) {
                if (j == 0 && mockgrid[j][i] == 2) {
                    score += _TOUCHING_WALLS;
                }
                if (j + 1 == TetrisEngine.WIDTH - 1 && mockgrid[j + 1][i] == 2) {
                    score += _TOUCHING_WALLS;
                }
                if (mockgrid[j][i] + mockgrid[j + 1][i] >= 3) {
                    score += _TOUCHING_EDGES;
                }
            }
        }

        //vertical pairs
        for (int i = 0; i < TetrisEngine.WIDTH; i++) {
            for (int j = 0; j < TetrisEngine.HEIGHT - 1; j++) {
                if (j + 1 == TetrisEngine.HEIGHT - 1 && mockgrid[i][j + 1] == 2) {
                    score += _TOUCHING_FLOOR;
                }
                if (mockgrid[i][j] + mockgrid[i][j + 1] >= 3) {
                    score += _TOUCHING_EDGES;
                }
            }
        }

        // Penalize HEIGHT.
        for (int i = 0; i < TetrisEngine.WIDTH; i++) {
            for (int j = 0; j < TetrisEngine.HEIGHT; j++) {
                int curheight = TetrisEngine.HEIGHT - j;
                if (mockgrid[i][j] > 0) {
                    score += curheight * _HEIGHT;
                }
            }
        }

        //Penalize holes. Also penalize blocks above holes.
        for (int i = 0; i < TetrisEngine.WIDTH; i++) {
            // Part 1: Count how many holes (space beneath blocks)
            boolean f = false;
            int holes = 0;
            for (int j = 0; j < TetrisEngine.HEIGHT; j++) {
                if (mockgrid[i][j] > 0) {
                    f = true;
                }
                if (f && mockgrid[i][j] == 0) {
                    holes++;
                }
            }

            // Part 2: Count how many blockades (block above space)
            f = false;
            int blockades = 0;
            for (int j = TetrisEngine.HEIGHT - 1; j >= 0; j--) {
                if (mockgrid[i][j] == 0) {
                    f = true;
                }
                if (f && mockgrid[i][j] > 0) {
                    blockades++;
                }
            }

            score += _HOLES * holes;
            score += _BLOCKADE * blockades;
        }

        score += cleared * _CLEAR;

        /*
         * for (int i1 = 0; i1 < mockgrid.length; i1++) { for (int i2 = 0; i2 <
         * mockgrid[0].length; i2++) { System.out.print(mockgrid[i1][i2] + " ");
         * } System.out.println(); }
		System.out.println(score);
         */
        //System.exit(0);

        return score;
    }
}