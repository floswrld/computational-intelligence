
public class Individual {
	int[]  perm;    // SOLUTION: a permutation of the jobs 0..n-1
	int    fitness; // SOLUTION-QUALITY (to be minimized)
	double p_MUT;   // MUTATION-PROBABILITY of EACH POSITION
	int    problemsize;

	public Individual(int problemsize){
		this.problemsize = problemsize;
		perm    = new int[problemsize];
		p_MUT   = 1./perm.length;
	}

	public void resetMutationProbability(){
		this.p_MUT = 1./perm.length;
	}

	public void adjustMutationProbability(){
		if (this.p_MUT * 2 <= 0.3) { // max mutation probability
			this.p_MUT = this.p_MUT * 2;
		}
	}

	// Swap mutation: keeps the chromosome a valid permutation
	public void mutation(){
		for(int i=0;i<perm.length;i++){
			if(Math.random() < p_MUT){
				int k    = (int)(Math.random()*perm.length);
				int tmp  = perm[i];
				perm[i]  = perm[k];
				perm[k]  = tmp;
			}
		}
	}

	// Order Crossover (OX): permutation-preserving two-child crossover
	public static void crossover(Individual papa, Individual mama, Individual son, Individual daughter){
		int len = papa.perm.length;
		int i   = (int)(Math.random()*len);
		int j   = (int)(Math.random()*len);
		if(i > j){ int tmp = i; i = j; j = tmp; } // segment [i..j]

		orderCrossover(papa.perm, mama.perm, son.perm,      i, j);
		orderCrossover(mama.perm, papa.perm, daughter.perm, i, j);
	}

	private static void orderCrossover(int[] p1, int[] p2, int[] child, int i, int j){
		int len = p1.length;
		boolean[] used = new boolean[len];

		for(int k=i;k<=j;k++){
			child[k]   = p1[k];
			used[p1[k]] = true;
		}

		int pos = (j+1) % len;
		for(int c=0;c<len;c++){
			int gene = p2[(j+1+c) % len];
			if(!used[gene]){
				child[pos] = gene;
				used[gene] = true;
				pos = (pos+1) % len;
			}
		}
	}

	public void output(){
		for(int i=0;i<perm.length;i++){
			System.out.print(perm[i] + " ");
		}
		System.out.println("- fitness " + fitness);
	}

	// random permutation (Fisher-Yates)
	public void initialize(){
		for(int i=0;i<perm.length;i++) perm[i] = i;
		for(int i=perm.length-1;i>0;i--){
			int k   = (int)(Math.random()*(i+1));
			int tmp = perm[i];
			perm[i] = perm[k];
			perm[k] = tmp;
		}
	}

	public void fitness(){
		fitness = Problem.fitness(perm);
	}

	public void reproduce(Individual template){
		for(int i=0;i<perm.length;i++){
			this.perm[i] = template.perm[i];
		}
		this.fitness = template.fitness;
		this.p_MUT   = template.p_MUT;
	}
}
