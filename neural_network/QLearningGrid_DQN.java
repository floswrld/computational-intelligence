import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QLearningGrid_DQN {

    static final int    WIDTH      = 11;
    static final int    HEIGHT     = 11;
    static final int    ACTIONS    = 4;
    static final int    EPISODES   = 25000;
    static final int    MAX_STEPS  = 200;

    static final int    HIDDEN     = 128;
    static final double LR         = 0.05;
    static final double GAMMA      = 0.95;

    static final double EPSILON_START = 1.0;
    static final double EPSILON_END   = 0.05;
    static final double EPSILON_DECAY = 0.99;

    static final int    REPLAY_CAP    = 20000;
    static final int    BATCH         = 64;
    static final int    WARMUP        = 500;
    static final int    TARGET_SYNC   = 500;

    static final int    START_X    = 0;
    static final int    START_Y    = 0;
    static final int    GOAL_X     = WIDTH - 1;
    static final int    GOAL_Y     = HEIGHT - 1;

    static final double REWARD_GOAL = 1.0;
    static final double REWARD_STEP = -0.02;
    static final double REWARD_BUMP = -0.05;


    static final int EVAL_EVERY = 5;
    static final int PATIENCE   = 140;

    static final int    INPUT = WIDTH * HEIGHT;

    static boolean[][] obstacle = new boolean[WIDTH][HEIGHT];

    static NeuralNetwork online;
    static NeuralNetwork target;

    static double epsilon = EPSILON_START;
    static Random rand = new Random(7);

    static int[]     replayS  = new int[REPLAY_CAP];
    static int[]     replayA  = new int[REPLAY_CAP];
    static double[]  replayR  = new double[REPLAY_CAP];
    static int[]     replaySN = new int[REPLAY_CAP];
    static boolean[] replayD  = new boolean[REPLAY_CAP];
    static int replaySize = 0;
    static int replayPos  = 0;

    static int trainSteps = 0;

    static HeatmapVisualizer heatmap;

    public static void main(String[] args) throws InterruptedException {
        // build labyrinth
        buildObstacles();

        // create the online, target and best-so-far networks (same initial weights)
        online = new NeuralNetwork(INPUT, HIDDEN, ACTIONS, LR, 7);
        target = new NeuralNetwork(INPUT, HIDDEN, ACTIONS, LR, 7);
        NeuralNetwork best = new NeuralNetwork(INPUT, HIDDEN, ACTIONS, LR, 7);
        target.copyFrom(online);
        best.copyFrom(online);

        // open the heatmap window
        heatmap = new HeatmapVisualizer(WIDTH, HEIGHT, START_X, START_Y, GOAL_X, GOAL_Y, obstacle);
        heatmap.update(buildQTable());
        Thread.sleep(50);

        // bestLen = shortest greedy path found so far, lastImproveEp = episode of the last improvement
        int bestLen = Integer.MAX_VALUE;
        int lastImproveEp = 0;

        // outer loop: one training episode per iteration
        for (int ep = 1; ep <= EPISODES; ep++) {

            // run one episode
            runEpisode();

            // decay exploration toward its minimum
            epsilon = Math.max(EPSILON_END, epsilon * EPSILON_DECAY);

            // evaluate the greedy policy every EVAL_EVERY episodes
            if (ep % EVAL_EVERY == 0 || ep == 1 || ep == EPISODES) {
                int evalSteps = evaluate();

                // keep the best result: store its length and snapshot the network
                if (evalSteps >= 0 && evalSteps < bestLen) {
                    bestLen = evalSteps;
                    lastImproveEp = ep;
                    best.copyFrom(online);
                }
                // log
                System.out.printf("Episode %4d   epsilon=%.3f   greedySteps=%s   best=%s   sinceImprove=%d%n",
                        ep, epsilon,
                        evalSteps < 0 ? "fail" : Integer.toString(evalSteps),
                        bestLen == Integer.MAX_VALUE ? "-" : Integer.toString(bestLen),
                        ep - lastImproveEp);
                heatmap.update(buildQTable());
                Thread.sleep(15);

                // early stopping: stop once there was no improvement for PATIENCE episodes
                if (bestLen != Integer.MAX_VALUE && ep - lastImproveEp >= PATIENCE) {
                    break;
                }
            }
        }

        // load the best network found during training
        online.copyFrom(best);

        // draw the final policy and the learned path
        heatmap.setPolicy(buildPolicy());
        heatmap.setPath(greedyPath());
        heatmap.update(buildQTable());

        // print the greedy path from start to goal
        System.out.println();
        //printGreedyPath();
    }

    static void buildObstacles() {
        obstacle = generateMaze(WIDTH, HEIGHT, System.nanoTime());
    }

    static boolean[][] generateMaze(int width, int height, long seed) {
        boolean[][] wall = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                wall[x][y] = true;
            }
        }
        Random rand = new Random(seed);
        carve(0, 0, wall, width, height, rand);

        int gx = width - 1, gy = height - 1;
        int ex = gx - (gx % 2), ey = gy - (gy % 2);
        wall[ex][ey] = false;
        wall[gx][ey] = false;
        wall[gx][gy] = false;
        wall[0][0]   = false;
        addLoops(wall, width, height, gx, gy, rand);
        return wall;
    }

    static void addLoops(boolean[][] wall, int width, int height, int gx, int gy, Random rand) {
        // Wände sammeln, die genau zwei gegenüberliegende offene Zellen trennen
        List<int[]> removable = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!wall[x][y]) continue;
                boolean lr = x - 1 >= 0 && x + 1 < width  && !wall[x - 1][y] && !wall[x + 1][y];
                boolean tb = y - 1 >= 0 && y + 1 < height && !wall[x][y - 1] && !wall[x][y + 1];
                if (lr ^ tb) removable.add(new int[]{x, y});
            }
        }
        // mischen (mit demselben rand, bleibt reproduzierbar)
        for (int i = removable.size() - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int[] t = removable.get(i);
            removable.set(i, removable.get(j));
            removable.set(j, t);
        }

        int target = 2 + rand.nextInt(3);               // Ziel: 2, 3 oder 4 Wege
        int count  = countPaths(wall, width, height, gx, gy);

        for (int[] w : removable) {
            if (count >= target) break;
            wall[w[0]][w[1]] = false;                   // Wand testweise oeffnen
            int nc = countPaths(wall, width, height, gx, gy);
            if (nc > count && nc <= 4) {
                count = nc;                             // behalten
            } else {
                wall[w[0]][w[1]] = true;                // kein Gewinn oder zu viele -> zurueck
            }
        }
    }

    static final int CAP = 5;                           // ab 5 zaehlen wir nicht weiter
    static final int[] DX = {1, -1, 0, 0};
    static final int[] DY = {0, 0, 1, -1};

    static int countPaths(boolean[][] wall, int width, int height, int gx, int gy) {
        boolean[][] vis = new boolean[width][height];
        int[] count = {0};
        vis[0][0] = true;
        dfs(0, 0, gx, gy, wall, vis, width, height, count);
        return count[0];
    }

    static void dfs(int x, int y, int gx, int gy, boolean[][] wall, boolean[][] vis,
                    int width, int height, int[] count) {
        if (count[0] >= CAP) return;                    // genug gezaehlt
        if (x == gx && y == gy) { count[0]++; return; }
        for (int i = 0; i < 4; i++) {
            int nx = x + DX[i], ny = y + DY[i];
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
            if (wall[nx][ny] || vis[nx][ny]) continue;
            vis[nx][ny] = true;
            dfs(nx, ny, gx, gy, wall, vis, width, height, count);
            vis[nx][ny] = false;
        }
    }

    static void carve(int x, int y, boolean[][] wall, int width, int height, Random rand) {
        wall[x][y] = false;
        int[][] dirs = {{0, -2}, {0, 2}, {-2, 0}, {2, 0}};
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int[] t = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = t;
        }
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < width && ny >= 0 && ny < height && wall[nx][ny]) {
                wall[x + d[0] / 2][y + d[1] / 2] = false;
                carve(nx, ny, wall, width, height, rand);
            }
        }
    }

    static int[] randomStart() {
        while (true) {
            int x = rand.nextInt(WIDTH);
            int y = rand.nextInt(HEIGHT);
            if (!obstacle[x][y] && !(x == GOAL_X && y == GOAL_Y)) {
                return new int[]{x, y};
            }
        }
    }

    static int encodeIdx(int x, int y) {
        return x * HEIGHT + y;
    }

    static int[] runEpisode() {

        // determine random start for episode
        int[] s0 = randomStart();
        int x = s0[0], y = s0[1];
        int steps = 0;

        while (!(x == GOAL_X && y == GOAL_Y) && steps < MAX_STEPS) {
            // encode cell to one-hot-vector
            int s = encodeIdx(x, y);
            // choose action based on NN
            int action = chooseAction(s);

            int nx = x, ny = y;
            switch (action) {
                case 0: ny = y - 1; break;
                case 1: ny = y + 1; break;
                case 2: nx = x - 1; break;
                case 3: nx = x + 1; break;
            }

            boolean blocked = nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT || obstacle[nx][ny];

            // action blocked? stay there
            if (blocked) {
                nx = x;
                ny = y;
            }

            // determine if goal reached
            boolean done = (nx == GOAL_X && ny == GOAL_Y);
            // calculate reward - if done 1.0 else if step taken -0.02 else if blocked -0.05
            double reward = done ? REWARD_GOAL : (blocked ? REWARD_BUMP : REWARD_STEP);

            // store the transition in the replay buffer and do one learning step
            store(s, action, reward, encodeIdx(nx, ny), done);
            learn();

            // move the agent to the new state and count the step
            x = nx;
            y = ny;
            steps++;
        }

        int reached = (x == GOAL_X && y == GOAL_Y) ? 1 : 0;
        return new int[]{steps, reached};
    }

    static int chooseAction(int s) {

        // choose random action
        if (rand.nextDouble() < epsilon) {
            return rand.nextInt(ACTIONS);
        }

        // predict each action for a cell and choose best
        return argmax(online.predict(s));
    }

    static void store(int s, int a, double r, int sn, boolean d) {
        // write the transition (state, action, reward, next state, done) at the current position
        replayS[replayPos]  = s;
        replayA[replayPos]  = a;
        replayR[replayPos]  = r;
        replaySN[replayPos] = sn;
        replayD[replayPos]  = d;
        // advance the write-position, wrapping around (ring buffer)
        replayPos = (replayPos + 1) % REPLAY_CAP;
        // grow the buffer size until it reaches its capacity
        if (replaySize < REPLAY_CAP) {
            replaySize++;
        }
    }

    static void learn() {
        // wait until enough experience is collected before training
        if (replaySize < WARMUP) {
            return;
        }

        int[] idxs = new int[BATCH];
        int[] acts = new int[BATCH];
        double[] targets = new double[BATCH];

        // calculate temporal difference value for a batch of the current chosen actions
        for (int b = 0; b < BATCH; b++) {
            // sample a random transition from the replay buffer
            int j = rand.nextInt(replaySize);
            idxs[b] = replayS[j];
            acts[b] = replayA[j];

            // target = reward, plus discounted best next value if the state is not terminal
            double t = replayR[j];
            if (!replayD[j]){
                t += GAMMA * max(target.predict(replaySN[j]));
            }
            targets[b] = t;
        }
        // one gradient step on the online network
        online.trainBatch(idxs, acts, targets);

        // periodically copy the online weights into the target network
        trainSteps++;
        if (trainSteps % TARGET_SYNC == 0) {
            target.copyFrom(online);
        }
    }

    static int evaluate() {
        List<int[]> path = greedyPath();
        int[] last = path.get(path.size() - 1);
        if (last[0] == GOAL_X && last[1] == GOAL_Y) {
            return path.size() - 1;
        }
        return -1;
    }

    static double[][][] buildQTable() {
        double[][][] Q = new double[WIDTH][HEIGHT][ACTIONS];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (obstacle[x][y]) continue;
                double[] q = online.predict(encodeIdx(x, y));
                for (int a = 0; a < ACTIONS; a++) Q[x][y][a] = q[a];
            }
        }
        return Q;
    }

    static int[][] buildPolicy() {
        int[][] policy = new int[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (obstacle[x][y] || (x == GOAL_X && y == GOAL_Y)) { policy[x][y] = -1; continue; }
                policy[x][y] = argmax(online.predict(encodeIdx(x, y)));
            }
        }
        return policy;
    }

    static List<int[]> greedyPath() {
        List<int[]> path = new ArrayList<>();
        boolean[][] seen = new boolean[WIDTH][HEIGHT];
        int x = START_X, y = START_Y;
        path.add(new int[]{x, y});

        for (int step = 0; step < WIDTH * HEIGHT * 4; step++) {
            if (x == GOAL_X && y == GOAL_Y) break;
            if (seen[x][y]) break;
            seen[x][y] = true;

            int action = argmax(online.predict(encodeIdx(x, y)));
            int nx = x, ny = y;
            switch (action) {
                case 0: ny = y - 1; break;
                case 1: ny = y + 1; break;
                case 2: nx = x - 1; break;
                case 3: nx = x + 1; break;
            }
            if (nx < 0 || nx >= WIDTH || ny < 0 || ny >= HEIGHT || obstacle[nx][ny]) break;

            x = nx; y = ny;
            path.add(new int[]{x, y});
        }
        return path;
    }

    static int argmax(double[] v) {
        int best = 0;
        for (int i = 1; i < v.length; i++) {
            if (v[i] > v[best]) {
                best = i;
            }
        }
        return best;
    }

    static double max(double[] v) {
        double m = v[0];
        for (int i = 1; i < v.length; i++) if (v[i] > m) m = v[i];
        return m;
    }

    static void printGreedyPath() {
        List<int[]> path = greedyPath();
        int[] last = path.get(path.size() - 1);
        boolean solved = last[0] == GOAL_X && last[1] == GOAL_Y;

        System.out.println("Learned greedy policy from start " + cell(START_X, START_Y)
                + " to goal " + cell(GOAL_X, GOAL_Y) + ":");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            int[] p = path.get(i);
            sb.append(cell(p[0], p[1]));
            if (i < path.size() - 1) sb.append(" -> ");
        }
        System.out.println(sb.toString());

        if (solved) System.out.println("Goal reached in " + (path.size() - 1) + " steps.");
        else        System.out.println("Goal not reached by greedy policy (path stopped after "
                + (path.size() - 1) + " steps).");

        System.out.println();
        printPolicyGrid();
    }

    static void printPolicyGrid() {
        int[][] policy = buildPolicy();
        String[] arrows = {"^", "v", "<", ">"};
        System.out.println("Policy grid (rows = y top..bottom, cols = x left..right):");
        for (int y = 0; y < HEIGHT; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < WIDTH; x++) {
                if (obstacle[x][y])                    row.append(" # ");
                else if (x == GOAL_X && y == GOAL_Y)   row.append(" G ");
                else if (x == START_X && y == START_Y) row.append(" S ");
                else                                   row.append(" ").append(arrows[policy[x][y]]).append(" ");
            }
            System.out.println(row.toString());
        }
    }

    static String cell(int x, int y) {
        return "(" + x + "," + y + ")";
    }
}
