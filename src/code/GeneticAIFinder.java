package code;

import java.util.*;

/*
 * A genetic algorithm to find combinations for AI values. This is an interface to the rest
 * of JTetris: they start by calling setAIValues() to let us set some values for the AI, then
 * they call sendScore() to give us what they got.
 */
public class GeneticAIFinder {
	
	// If false, just use the default values
	final boolean USE_GENETIC = false;
	
	// Which generation are we in?
	int generation = 1;
	
	// How many candidates are there in a generation?
	// Must be a multiple of 4.
	final int population = 16;
	
	// How often do chromosomes mutate?
	double mutation_rate = 0.05;
	
	// A chromosome is just an array of 7 doubles.
	double[][] chromosomes = new double[population][7];
	int[] scores = new int[population];
	
	Random rnd;
	TetrisEngine tetris;
	int current = 0;
	
	public GeneticAIFinder(TetrisEngine tetris){
		this.tetris = tetris;
		
		// Randomize starting chromosomes with values between -5 and 5.
		rnd = new Random();
		for(int i=0; i<population; i++){
			for(int j=0; j<7; j++){
				chromosomes[i][j] = rnd.nextDouble()*10 - 5;
			}
		}
	
	}
	
	void newGeneration(){
		
		// Calculate average fitness
		int[] scores_ = new int[population];
		for(int i=0; i<scores.length; i++) scores_[i] = scores[i];
		Arrays.sort(scores_);
		System.out.println("Generation " + generation + 
				"; min = " + scores_[0] +
				"; med = " + scores_[population/2] +
				"; max = " + scores_[population-1]);
		
		List<double[]> winners = new ArrayList<double[]>();
		
		// Pair 1 with 2, 3 with 4, etc.
		for(int i=0; i<(population/2); i++){
			
			// Pick the more fit of the two pairs
			int c1score = scores[i];
			int c2score = scores[i+1];
			int winner = c1score > c2score? i : i+1;
			
			// Keep the winner, discard the loser.
			winners.add(chromosomes[winner]);
		}
		
		
		int counter = 0;
		List<double[]> new_population = new ArrayList<double[]>();
		
		// Pair up two winners at a time
		for(int i=0; i<(winners.size()/2); i++){
			double[] winner1 = winners.get(i);
			double[] winner2 = winners.get(i+1);
			
			// Generate four new offspring
			for(int off=0; off<4; off++){
				
				double[] child = new double[7];
				
				// Pick at random a mixed subset of the two winners and make it the new chromosome
				for(int j=0; j<7; j++){
					child[j] = rnd.nextInt(2)>0 ? winner1[j] : winner2[j];
					
					// Chance of mutation
					boolean mutate = rnd.nextDouble() < mutation_rate;
					if(mutate){
						// Change this value anywhere from -5 to 5
						double change = rnd.nextDouble()*10 - 5;
						child[j] += change;
					}
				}
				
				new_population.add(child);
				counter++;
			}
		}
		
		// Shuffle the new population.
		Collections.shuffle(new_population, rnd);
		
		// Copy them over
		for(int i=0; i<population; i++){
			for(int j=0; j<7; j++)
				chromosomes[i][j] = new_population.get(i)[j];
		}
		
		generation++;
		current = 0;
		
	}
	
	void setAIValues(TetrisAI ai){
		if(!USE_GENETIC) return;
		
		ai._TOUCHING_EDGES = chromosomes[current][0];
		ai._TOUCHING_WALLS = chromosomes[current][1];
		ai._TOUCHING_FLOOR = chromosomes[current][2];
		ai._HEIGHT = chromosomes[current][3];
		ai._HOLES = chromosomes[current][4];
		ai._BLOCKADE = chromosomes[current][5];
		ai._CLEAR = chromosomes[current][6];
	}
	
	void sendScore(int score){
		if(!USE_GENETIC) return;
		
		String s = aToS(chromosomes[current]);
		s = "Generation " + generation + "; Candidate " + (current+1) + ": " + s + " Score = " + score;
		System.out.println(s);
		scores[current] = score;
		current++;
		
		if(current == population)
			newGeneration();
	}
	
	// Double array to string, two decimal places
	private String aToS(double[] a){
		String s = "";
		for(int i=0; i<a.length; i++){
			s += Double.toString(((double)Math.round(a[i]*100))/100);
			if(i != a.length-1) s += ", ";
		}
		return "[" + s + "]";
	}
}
