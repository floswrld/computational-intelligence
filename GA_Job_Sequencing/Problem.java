import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Problem {

	// problembezogene Daten
	static int     n;        //Number of Jobs (= length of the sequence)
	static int     m;        //Number of Machines
	static int[][] time;     //Processing time of each job on each machine
	static int[][] delay;    //Delay if job j directly follows job h
	static int     timeSum;  //Constant: sum of all processing times

	public static void readInstance(String dateiName){
		// Aufbau der Datei:
		// Zeile 1: Anzahl Jobs
		// Zeile 2: Anzahl Maschinen
		// danach: je Zeile die Bearbeitungszeiten eines Jobs auf den Maschinen

		try {
			Scanner scanner = new Scanner(new File(dateiName));
			n = scanner.nextInt();
			m = scanner.nextInt();
			time = new int[n][m];
			timeSum = 0;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < m; j++) {
					time[i][j] = scanner.nextInt();
					timeSum += time[i][j];
				}
			}
			scanner.close();
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException("Instanz nicht gefunden: " + dateiName);
		}

		calculateDelay();
	}

	// flow-shop end times: end[positionInSequence][machine]
	// a job enters machine k only after it finished machine k-1
	// and after the previous job in the sequence left machine k
	public static int[][] endTimes(int[] perm){
		int[][] end = new int[n][m];
		for (int i = 0; i < n; i++) {
			int job = perm[i];
			for (int k = 0; k < m; k++) {
				int prevMachine = (k == 0) ? 0 : end[i][k - 1];
				int prevJob     = (i == 0) ? 0 : end[i - 1][k];
				int start       = Math.max(prevMachine, prevJob);
				end[i][k] = start + time[job][k];
			}
		}
		return end;
	}

	public static int makespan(int[] perm){
		int[][] end = endTimes(perm);
		return end[n - 1][m - 1];
	}

	private static void calculateDelay(){
		delay = new int[n][n];
		for (int h = 0; h < n; h++) {
			for (int j = 0; j < n; j++) {
				delay[h][j] = 0;
				if (h != j) {
					int maxWait = 0;
					for (int machine = 0; machine < m; machine++) {
						int time1 = 0;
						for (int k = 0; k <= machine; k++) {
							time1 += time[h][k];
						}
						int time2 = 0;
						for (int k = 1; k <= machine; k++) {
							time2 += time[j][k - 1];
						}
						int wait = Math.max(time1 - time2, 0);
						if (wait > maxWait)
							maxWait = wait;
					}
					delay[h][j] = maxWait;
				}
			}
		}
	}

	public static int fitness(int[] perm){
		int weightSum = timeSum;
		for (int i = 1; i < perm.length; i++) { // starte bei zweitem Job
			int jobVor = perm[i - 1];
			int job    = perm[i];
			int multi  = perm.length - i;
			weightSum += (delay[jobVor][job] * multi);
		}
		return weightSum;
	}
}
