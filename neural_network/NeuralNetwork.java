import java.util.Random;

public class NeuralNetwork {

    int nIn, nHidden, nOut;
    double[][] W1;
    double[]   b1;
    double[][] W2;
    double[]   b2;
    double lr;
    Random rand;

    public NeuralNetwork(int nIn, int nHidden, int nOut, double lr, long seed) {
        this.nIn = nIn;
        this.nHidden = nHidden;
        this.nOut = nOut;
        this.lr = lr;
        this.rand = new Random(seed);

        // Graphs between input layer and hidden layer
        W1 = new double[nHidden][nIn];
        // biases of hidden layer
        b1 = new double[nHidden];
        // Graphs between hidden layer and output layer
        W2 = new double[nOut][nHidden];
        // biases of output layer
        b2 = new double[nOut];

        // weights are normally distributed and scaled with sqrt(2/n) -> no values get to big or get lost when running through ReLu layer biases start at 0
        double s1 = Math.sqrt(2.0 / nIn);
        double s2 = Math.sqrt(2.0 / nHidden);

        // give randomised weights to graphs
        for (int j = 0; j < nHidden; j++)
            for (int i = 0; i < nIn; i++)
                W1[j][i] = rand.nextGaussian() * s1;
        for (int k = 0; k < nOut; k++)
            for (int j = 0; j < nHidden; j++)
                W2[k][j] = rand.nextGaussian() * s2;
    }

    /**
     * Predict the best direction of a given cell
     *
     * @param idx One-Hot-Vector - 100 0's and a single 1 for the current cell
     * @return
     */
    public double[] predict(int idx) {
        // calculate Q as z for every hidden neuron
        double[] a1 = new double[nHidden];
        for (int j = 0; j < nHidden; j++) {
            // weigh of graph plus bias
            double z = b1[j] + W1[j][idx];
            // ReLu to make network non-linear
            a1[j] = z > 0 ? z : 0;
        }

        // calculate Q as z for every output
        double[] o = new double[nOut];
        for (int k = 0; k < nOut; k++) {
            // get bias of output neuron
            double z = b2[k];
            for (int j = 0; j < nHidden; j++) {
                // add sum of layer before
                z += W2[k][j] * a1[j];
            }
            o[k] = z;
        }
        return o;
    }

    /**
     * Adjust weights of graphs of the NN - backtracking error from output layer to hidden layer and from hidden layer to input layer
     * @param idxs
     * @param actions
     * @param targets
     */
    public void trainBatch(int[] idxs, int[] actions, double[] targets) {
        int m = idxs.length;

        // Graphs between input layer and hidden layer
        double[][] gW1 = new double[nHidden][nIn];
        // biases of hidden layer
        double[]   gb1 = new double[nHidden];
        // Graphs between hidden layer and output layer
        double[][] gW2 = new double[nOut][nHidden];
        // biases of output layer
        double[]   gb2 = new double[nOut];

        // loop over every sample in the minibatch
        for (int s = 0; s < m; s++) {
            // one-hot index of the state and the action that was taken
            int idx = idxs[s];
            int a = actions[s];

            // forward pass into the hidden layer (one-hot, so only column idx of W1 contributes)
            double[] z1 = new double[nHidden];
            double[] a1 = new double[nHidden];
            for (int j = 0; j < nHidden; j++) {
                double z = b1[j] + W1[j][idx];
                z1[j] = z;
                // ReLU activation
                a1[j] = z > 0 ? z : 0;
            }

            // output value only for the chosen action a
            double oa = b2[a];
            for (int j = 0; j < nHidden; j++) {
                oa += W2[a][j] * a1[j];
            }

            // error = prediction minus TD-target
            double err = oa - targets[s];
            // clip the error for stability
            if (err > 1.0) err = 1.0;
            if (err < -1.0) err = -1.0;

            // gradients of the output layer (only the chosen action a)
            for (int j = 0; j < nHidden; j++) {
                gW2[a][j] += err * a1[j];
            }
            gb2[a] += err;

            // backprop into the hidden layer
            for (int j = 0; j < nHidden; j++) {
                // ReLU derivative: skip neurons that were not active
                if (z1[j] <= 0) {
                    continue;
                }
                double d1 = err * W2[a][j];
                // one-hot input, so only column idx gets a gradient
                gW1[j][idx] += d1;
                gb1[j] += d1;
            }
        }

        // average over the minibatch and scale by the learning rate
        double scale = lr / m;
        // update input->hidden weights and hidden biases
        for (int j = 0; j < nHidden; j++) {
            for (int i = 0; i < nIn; i++) {
                if (gW1[j][i] != 0) {
                    W1[j][i] -= scale * gW1[j][i];
                }
            }
            b1[j] -= scale * gb1[j];
        }
        // update hidden->output weights and output biases
        for (int k = 0; k < nOut; k++) {
            for (int j = 0; j < nHidden; j++) {
                W2[k][j] -= scale * gW2[k][j];
            }
            b2[k] -= scale * gb2[k];
        }
    }

    public void copyFrom(NeuralNetwork other) {
        for (int j = 0; j < nHidden; j++) {
            System.arraycopy(other.W1[j], 0, this.W1[j], 0, nIn);
        }
        System.arraycopy(other.b1, 0, this.b1, 0, nHidden);
        for (int k = 0; k < nOut; k++) {
            System.arraycopy(other.W2[k], 0, this.W2[k], 0, nHidden);
        }
        System.arraycopy(other.b2, 0, this.b2, 0, nOut);
    }
}
