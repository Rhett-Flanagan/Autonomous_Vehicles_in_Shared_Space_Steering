package train;


import intersectionmanagement.trial.Trial;
import org.apache.commons.lang3.SerializationUtils;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void compareToHeuristic(NEATNetwork network){

    }

    public static void main(String[] args) throws IOException {
        String parameters = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        int iterations = Integer.parseInt(args[1]);
        int popSize = Integer.parseInt(args[2]);
        int sims = Integer.parseInt(args[3]);

        NEATPopulation population = new NEATPopulation(14, 2, popSize);
        CarControllerScore score = new CarControllerScore(parameters, sims);

        EvolutionaryAlgorithm train = NEATUtil.constructNEATTrainer(population, score);

        for(int i = 0; i < iterations; i++){
            train.iteration();
            System.out.println("Epoch #" + train.getIteration() + " Error:" + train.getError()+ ", Species:" + population.getSpecies().size());
        }

        NEATNetwork network = (NEATNetwork)train.getCODEC().decode((train.getBestGenome()));

        Trial trial = new Trial(parameters, SerializationUtils.serialize(network));
        for(int i = 0; i < 10; i++){
            trial.setupSim();
            int collisions = trial.runSimulation();
            System.out.println("Run " + (i + 1) + " collisions: " + collisions);
        }
    }
}
