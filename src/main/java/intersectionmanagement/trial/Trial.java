package intersectionmanagement.trial;

import intersectionmanagement.simulator.Actor;
import intersectionmanagement.simulator.Simulator;
import intersectionmanagement.simulator.Utility;
import intersectionmanagement.simulator.car.Car;
import intersectionmanagement.simulator.obstacle.Obstacle;
import intersectionmanagement.simulator.pedestrian.Pedestrian;
import intersectionmanagement.simulator.spawner.CarSpawner;
import intersectionmanagement.simulator.spawner.PedestrianSpawner;
import intersectionmanagement.simulator.track.Node;
import intersectionmanagement.simulator.track.TrackParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static intersectionmanagement.simulator.spawner.CarSpawner.Function.*;

public class Trial {
    private static final Logger LOGGER = Logger.getLogger(Trial.class.getName());
    // CLI option for LWJGL
    //-Djava.library.path=Trial/target/natives

    private final int seed;
    private final String trackFile;
    private final SpawnerFactory spawnerFactory;
    private final int simulationSteps;
    private final byte[] serializedNetwork;

    private final int pedestrianRate;
    private final float pedestrianRandomness;

    private Simulator sim;
    private List<Node> track;

    private boolean simulating = true;

    public Trial(String parameters) {
        JSONObject jsonParameters = new JSONObject(parameters);
        seed = jsonParameters.getInt("seed");
        trackFile = jsonParameters.getString("track");
        simulationSteps = jsonParameters.getInt("steps");
        JSONArray jsonSerializedNetwork = jsonParameters.getJSONArray("neural_network");
        serializedNetwork = new byte[jsonSerializedNetwork.length()];
        for (int i = 0; i < jsonSerializedNetwork.length(); i++) {
            serializedNetwork[i] = (byte) jsonSerializedNetwork.getInt(i);
        }

        JSONObject spawner = jsonParameters.getJSONObject("spawner");
        String spawnerType = spawner.getString("type");
        double randomness = spawner.getDouble("randomness");
        pedestrianRandomness = spawner.getFloat("pedestrian_randomness");
        pedestrianRate = spawner.getInt("pedestrian_rate");
        SpawnerFactory spawnerFactory;
        switch (spawnerType) {
            case "constant":
                double[] params = new double[1];
                params[0] = spawner.getInt("period");
                spawnerFactory = new SpawnerFactory(CONSTANT, serializedNetwork, simulationSteps, params, randomness);
                break;
            case "linear":
                params = new double[2];
                params[0] = spawner.getInt("min_period");
                params[1] = spawner.getInt("max_period");
                spawnerFactory = new SpawnerFactory(LINEAR, serializedNetwork, simulationSteps, params, randomness);
                break;
            case "sin":
                params = new double[3];
                params[0] = spawner.getDouble("period_mul");
                params[1] = spawner.getInt("min_period");
                params[2] = spawner.getInt("max_period");
                spawnerFactory = new SpawnerFactory(SIN, serializedNetwork, simulationSteps, params, randomness);
                break;
            default:
                LOGGER.severe(String.format("%s is not a valid spawner type", spawnerType));
                throw new RuntimeException("No valid spawner specified in trial parameters");
        }
        this.spawnerFactory = spawnerFactory;
    }

    public Trial(String parameters, byte[] serializedNetwork) {
        JSONObject jsonParameters = new JSONObject(parameters);
        this.serializedNetwork = serializedNetwork;
        seed = (int) (Math.random() * 1000);//jsonParameters.getInt("seed");
        trackFile = jsonParameters.getString("track");
        simulationSteps = jsonParameters.getInt("steps");
        JSONObject spawner = jsonParameters.getJSONObject("spawner");
        String spawnerType = spawner.getString("type");
        double randomness = spawner.getDouble("randomness");
        pedestrianRandomness = spawner.getFloat("pedestrian_randomness");
        pedestrianRate = spawner.getInt("pedestrian_rate");
        SpawnerFactory spawnerFactory;
        switch (spawnerType) {
            case "constant":
                double[] params = new double[1];
                params[0] = spawner.getInt("period");
                spawnerFactory = new SpawnerFactory(CONSTANT, serializedNetwork, simulationSteps, params, randomness);
                break;
            case "linear":
                params = new double[2];
                params[0] = spawner.getInt("min_period");
                params[1] = spawner.getInt("max_period");
                spawnerFactory = new SpawnerFactory(LINEAR, serializedNetwork, simulationSteps, params, randomness);
                break;
            case "sin":
                params = new double[3];
                params[0] = spawner.getDouble("period_mul");
                params[1] = spawner.getInt("min_period");
                params[2] = spawner.getInt("max_period");
                spawnerFactory = new SpawnerFactory(SIN, serializedNetwork, simulationSteps, params, randomness);
                break;
            default:
                LOGGER.severe(String.format("%s is not a valid spawner type", spawnerType));
                throw new RuntimeException("No valid spawner specified in trial parameters");
        }
        this.spawnerFactory = spawnerFactory;
    }

    public void setupSim() throws IOException {
        track = TrackParser.parseTrack(trackFile, false);
        sim = new Simulator(seed);
        for (Node startNode : track) {
            sim.addActor(spawnerFactory.getSpawner(sim, startNode));
        }

        List<Node> pedestrianTrack = TrackParser.parseTrack(trackFile, true);
        for (Node startNode : pedestrianTrack) {
            sim.addActor(new PedestrianSpawner(sim, startNode, simulationSteps, pedestrianRate, pedestrianRandomness));
        }

        addObstacles();
    }

    public void addObstacles() throws IOException {
        JSONObject trackJSON = new JSONObject(Utility.loadResource(trackFile));
        if (trackJSON.has("obstacles")) {
            JSONArray obstacles = trackJSON.getJSONArray("obstacles");
            Node startNode = track.get(0);
            for (Object obstacleJSON : obstacles) {
                JSONObject obstacle = (JSONObject) obstacleJSON;
                sim.addActor(new Obstacle(sim, startNode, obstacle.getFloat("x"), obstacle.getFloat("y"), obstacle.getFloat("radius")));
            }
        }
    }

    public int runSimulation() throws IOException {
        setupSim();

        for (int i = 0; i < simulationSteps; i++) {
            sim.step();
        }

        return sim.getCollisions();
    }

    public void runSimulationRendered() throws LWJGLException, IOException {
        ArrayList<Car> cars = new ArrayList<>();
        ArrayList<Pedestrian> pedestrians = new ArrayList<>();
        ArrayList<Obstacle> obstacles = new ArrayList<>();
        Renderer.setupWindow("*", this, 4, 800, 800, serializedNetwork);
        setupSim();

        while (!Display.isCloseRequested()) {
            if (simulating) {
                sim.step();
                cars = sim.getCars();
                pedestrians = sim.getPedestrians();
                obstacles = sim.getObstacles();
            }
            Renderer.drawActors(cars, pedestrians, obstacles, track);
            Renderer.handleInput(cars);

            if (Display.isCloseRequested()) {
                System.exit(0);
            }
        }
        Display.destroy();
    }

    public void toggleSimulating() {
        simulating = !simulating;
    }

    private class SpawnerFactory {
        private final CarSpawner.Function function;
        private final byte[] weights;
        private final int simulationSteps;
        private final double[] params;
        private final double randomDenominator;

        SpawnerFactory(CarSpawner.Function function, byte[] weights, int simulationSteps, double[] params, double randomDenominator) {
            this.function = function;
            this.weights = weights;
            this.simulationSteps = simulationSteps;
            this.params = params;
            this.randomDenominator = randomDenominator;
        }

        Actor getSpawner(Simulator sim, Node startNode) {
            return new CarSpawner(sim, startNode, weights, simulationSteps, function, params, randomDenominator);
        }
    }
}
