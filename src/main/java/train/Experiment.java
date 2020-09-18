package train;

import intersectionmanagement.trial.Trial;
import org.apache.commons.lang3.SerializationUtils;
import org.encog.Encog;
import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.hyperneat.substrate.Substrate;
import org.encog.neural.hyperneat.substrate.SubstrateFactory;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Experiment {

    static final String[] files = {"experimentparams/circle-params.json",
            "experimentparams/circle-small-params.json",
            "experimentparams/concentric-circle-params.json",
            "experimentparams/eight-way-circle-params.json",
            "experimentparams/pedestrian-priority-square-params.json",
            "experimentparams/four-way-2-lane-params.json"};

    public static void main(String[] args) throws IOException {
        int iterations = Integer.parseInt(args[0]);
        int popSize = Integer.parseInt(args[1]);
        int sims = Integer.parseInt(args[2]);

        NEATExperiment(iterations, popSize, sims);
//        hyperNEATExperiment(iterations, popSize, sims);

    }

    public static void NEATExperiment(int iterations, int popSize, int sims) throws IOException {
        JSONObject out = new JSONObject();
        for (int config = 0; config < 3; ++config) {
            for (String file : files) {

                String parameters = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
                // Setup trainer
                int inputs = 14;
                switch (config) {
                    case 0:
                        inputs = 14;
                        break;
                    case 1:
                        inputs = 16;
                        break;
                    case 2:
                        inputs = 18;
                        break;
                    default:
                        break;
                }

                NEATPopulation population = new NEATPopulation(inputs, 2, popSize);
                population.setInitialConnectionDensity(1.0);// not required, but speeds training
                population.reset();
                CarControllerScore score = new CarControllerScore(parameters, sims, config);


                // Train
                final EvolutionaryAlgorithm train = NEATUtil.constructNEATTrainer(population, score);
                //System.out.println("Loading complete, beginning training.");

                for (int i = 0; i < iterations; i++) {
                    train.iteration();
                    //System.out.println("Epoch #" + train.getIteration() + ", Collisions:" + train.getError() + ", Species:" + population.getSpecies().size());
                }

                // Retrieve and save the best network
                NEATNetwork network = (NEATNetwork) train.getCODEC().decode((train.getBestGenome()));
                Encog.getInstance().shutdown();


                // Run performance Tests
                JSONObject results = new JSONObject();
                String prefix = file.substring(17, file.length() - 12);
                byte[] net = SerializationUtils.serialize(network);
                results.put(prefix + "-network", new JSONArray(net));
                out.put(prefix + "-" + config, getResults(results, net, config));

                //out.put(parameters.substring(7, parameters.length() - 5) + "-network-" + config, new JSONArray(SerializationUtils.serialize(network)));
            }
        }

        try (FileWriter file = new FileWriter("out/NEAT_results.json", false)) {
            file.write(out.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void hyperNEATExperiment(int iterations, int popSize, int sims) throws IOException {
        JSONObject out = new JSONObject();
        for (int config = 0; config < 1; ++config) {
            for (String file : files) {
                // Setup trainer
                String parameters = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);

                int inputs = 14;
                switch (config) {
                    case 0:
                        inputs = 15;
                        break;
                    case 1:
                        inputs = 17;
                        break;
                    case 2:
                        inputs = 18;
                        break;
                    default:
                        break;
                }

                Substrate substrate = SubstrateFactory.factorSandwichSubstrate(inputs, 2);
                NEATPopulation population = new NEATPopulation(substrate, popSize);
                population.setActivationCycles(4);
                population.reset();
                CarControllerScore score = new CarControllerScore(parameters, sims, config);

                // Train
                final EvolutionaryAlgorithm train = NEATUtil.constructNEATTrainer(population, score);
                //System.out.println("Loading complete, beginning training.");

                for (int i = 0; i < iterations; i++) {
                    train.iteration();
                    //System.out.println("Epoch #" + train.getIteration() + ", Collisions:" + train.getError() + ", Species:" + population.getSpecies().size());
                }

                // Retrieve and save the best network
                NEATNetwork network = (NEATNetwork) train.getCODEC().decode((train.getBestGenome()));
                Encog.getInstance().shutdown();

                // Run performance Tests
                JSONObject results = new JSONObject();
                String prefix = file.substring(17, file.length() - 5);
                byte[] net = SerializationUtils.serialize(network);
                results.put(prefix + "-network", new JSONArray(net));
                out.put(prefix + "-" + config, getResults(results, net, config));
            }
            try (FileWriter file = new FileWriter("out/HyperNEAT_results.json", false)) {
                file.write(out.toString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONObject getResults(JSONObject results, byte[] network, int config) throws IOException {
        // Run performance Tests

        for (String file : files) {
            String parameters = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);

            String track = file.substring(17, file.length() - 5);
            double ave = 0;
            for (int i = 0; i < 5; i++) {
                Trial trial = new Trial(parameters, network, config);
                ave += trial.runSimulation();
            }
            ave /= 15;
            results.put(track + "-config-" + config + "-collisions", ave);
        }
        return results;
    }

}


