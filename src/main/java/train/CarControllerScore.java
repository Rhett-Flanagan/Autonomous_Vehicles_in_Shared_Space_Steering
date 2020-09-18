package train;

import intersectionmanagement.trial.Trial;
import org.apache.commons.lang3.SerializationUtils;
import org.encog.ml.CalculateScore;
import org.encog.ml.MLMethod;
import org.encog.neural.neat.NEATNetwork;

import java.io.IOException;
import java.io.Serializable;

/**
 * Calculate a score based on the number of collisions in the simulator
 */
public class CarControllerScore implements CalculateScore, Serializable {

    String parameters;
    int numSims, config;

    public CarControllerScore(String parameters, int numSims) {
        this.parameters = parameters;
        this.numSims = numSims;
        config = -1;
    }

    public CarControllerScore(String parameters, int numSims, int config) {
        this.parameters = parameters;
        this.numSims = numSims;
        this.config = config;
    }

    /**
     * Calculate this network's score by running a number of simulations, then taking the average number of collisions across all runs.
     *
     * @param method The NEATnetwork passed as a MLMethod.
     * @return The average across all runs.
     */
    public double calculateScore(MLMethod method) {
        NEATNetwork network = (NEATNetwork) method; // The "ML" method passed here should apparently be the network, not the genome

        double average = 0;

        for (int i = 0; i < numSims; i++) {
            try {
                Trial trial;
                if (config == -1)
                    trial = new Trial(parameters, SerializationUtils.serialize(network));
                else
                    trial = new Trial(parameters, SerializationUtils.serialize(network), config);
                trial.setupSim();
                average += trial.runSimulation();//fit;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return average / (double) numSims;
    }

    /**
     * @return Always true as we aim to minimise the number of collisions
     */
    public boolean shouldMinimize() {
        return true;
    }

    /**
     * @return Use a parallel approach
     */
    public boolean requireSingleThreaded() {
        return false;
    }
}
