package train;


import intersectionmanagement.trial.Trial;
import org.apache.commons.lang3.SerializationUtils;
import org.encog.Encog;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void compareToHeuristic(NEATNetwork network){

    }

    public static void main(String[] args) throws IOException {
        // Retrieve parameters
        String parameters = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        int iterations = Integer.parseInt(args[1]);
        int popSize = Integer.parseInt(args[2]);
        int sims = Integer.parseInt(args[3]);

        // Setup trainer
        NEATPopulation population = new NEATPopulation(14, 2, popSize);
        population.setInitialConnectionDensity(1.0);// not required, but speeds training
        population.reset();
        CarControllerScore score = new CarControllerScore(parameters, sims);

        // Train
        final EvolutionaryAlgorithm train = NEATUtil.constructNEATTrainer(population, score);
        System.out.println("Loading complete, beginning training.");
        for(int i = 0; i < iterations; i++){
            train.iteration();
            System.out.println("Epoch #" + train.getIteration() + ", Collisions:" + train.getError()+ ", Species:" + population.getSpecies().size());
        }

        // Retrieve and save the best network
        NEATNetwork network = (NEATNetwork)train.getCODEC().decode((train.getBestGenome()));
        Encog.getInstance().shutdown();
//        Trial trial = new Trial(parameters, SerializationUtils.serialize(network));
//        for(int i = 0; i < 10; i++){
//            trial.setupSim();
//            int collisions = trial.runSimulation();
//            System.out.println("Run " + (i + 1) + " collisions: " + collisions);
//        }

        // Save the network and parameters
        JSONObject jsonParameters = new JSONObject(parameters);
        //jsonParameters.remove("neural_network");
        jsonParameters.put("neural_network", new JSONArray(SerializationUtils.serialize(network)));

        try(FileWriter file = new FileWriter(args[0].substring(0, args[0].length() - 5)+ "_network.json")){
            file.write(jsonParameters.toString());
            file.flush();
        }catch (IOException e){
            e.printStackTrace();
        }


    }
}
