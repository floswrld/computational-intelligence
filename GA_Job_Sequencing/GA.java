public class GA {

	static int popSize             = 100; 			//Based on the problem size
	static int numberOfIterations  = 10_000;  		//Termination Criterion
	static int threshold 		   = 10;			//Threshold for adjusting mutation probability

	// allows the UI to receive progress and to stop a run
	public interface ProgressListener { void onIteration(int iter, int bestFitness); }
	static volatile boolean cancelRequested = false;

	public static void main(String[] args) {
		Problem.readInstance("data/daten3ACustomer_200_10.txt");
		//Problem.readInstance("data/datenCustomer_5_3.txt");

		Individual best = runGA(numberOfIterations, (it, f) -> System.out.println(it + " " + f));

		System.out.println();
		best.output();
	}

	// the genetic algorithm, identical loop as before, only parameterized + callback
	public static Individual runGA(int iterations, ProgressListener listener){
		cancelRequested = false;

		Individual best         = new Individual(Problem.n);
		best.initialize();
		best.fitness();
		Individual[] pop        = new Individual[popSize];
		Individual[] children   = new Individual[popSize];

		//1. Generate START-POPULATION
		for(int i=0;i<pop.length;i++){
			pop[i] = new Individual(Problem.n);
			pop[i].initialize();
			pop[i].fitness();
		}
		bestIndividual(pop, best);

		int counter = 0;

		for(int iter=1; iter<=iterations && !cancelRequested; iter++){

			for(int i=0;i<children.length;i=i+2){
				int parentIndex1 = selection(pop);
				int parentIndex2 = selection(pop);

				children[i]      = new Individual(Problem.n);
				children[i+1]    = new Individual(Problem.n);

				/*
				 * Adjust mutation probability based on the number of iterations without improvement
				 */
				if(counter > threshold){
					children[i].adjustMutationProbability();
					children[i+1].adjustMutationProbability();
				} else {
					children[i].resetMutationProbability();
					children[i+1].resetMutationProbability();
				}

				Individual.crossover(pop[parentIndex1], pop[parentIndex2], children[i], children[i+1]);

				children[i].mutation();
				children[i+1].mutation();

				children[i].fitness();
				children[i+1].fitness();
			}

			//Replacement
			pop        = children;
			pop[0] = new Individual(Problem.n); // Elitism
			pop[0].reproduce(best);

			children   = new Individual[popSize];

			if(!bestIndividual(pop, best)){
				counter++;
			} else {
				counter = 0;
			}

			if(listener != null) listener.onIteration(iter, best.fitness);
		}

		return best;
	}


	public static int selection(Individual[] list){
		int index = 0;

		//Tournement Selection for minimization!

		int index1 = (int)(Math.random()*list.length);
		int index2 = (int)(Math.random()*list.length);

		if(list[index1].fitness < list[index2].fitness){
			index = index1;
		}
		else{
			index = index2;
		}
		return index;
	}


	//return true is best is changed, false otherwise
	public static boolean bestIndividual(Individual[] liste, Individual best){
		boolean isBestChanged = false;
        for (Individual individual : liste) {
            if (individual.fitness < best.fitness) {
                best.reproduce(individual);
                isBestChanged = true;
            }
        }
		return isBestChanged;
	}

}
