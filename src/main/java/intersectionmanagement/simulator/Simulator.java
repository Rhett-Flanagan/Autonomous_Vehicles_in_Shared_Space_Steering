package intersectionmanagement.simulator;

import intersectionmanagement.simulator.car.Car;
import intersectionmanagement.simulator.obstacle.Obstacle;
import intersectionmanagement.simulator.pedestrian.Pedestrian;

import java.util.*;

public class Simulator {
    public static final int grid = 50;

    public ArrayList<Actor> actorArray;
    public ArrayList<Car> carArray;
    public ArrayList<Pedestrian> pedestrianArray;
    public ArrayList<Obstacle> obstacleArray;
    public ArrayList<Actor> newActorBuffer;

    public int collisions = 0;
    public int finishedCars = 0;
    public int spawnedCars = 0;

    public Random rng;

    public HashMap<Integer, HashMap<Integer, ArrayList<Actor>>> actorGrid;

    public List<double[]> controlHistory;

    public Simulator(long rngSeed) {
        rng = new Random(rngSeed);
        actorArray = new ArrayList<>();
        carArray = new ArrayList<>();
        pedestrianArray = new ArrayList<>();
        obstacleArray = new ArrayList<>();
        newActorBuffer = new ArrayList<>();

        controlHistory = new ArrayList<>();

        actorGrid = new HashMap<>();
        for (int i = -1; i < grid + 1; i++) {
            actorGrid.put(i, new HashMap<>());
            for (int j = -1; j < grid + 1; j++) {
                actorGrid.get(i).put(j, new ArrayList<>());
            }
        }
    }

    public void step() {
        ArrayList<Actor> removedActorArray = new ArrayList<>();

        for (Actor actor : actorArray) {
            actor.step(getActorsFromGrid(actor));
            actor.moveTowardsTarget();
            if (actor.finished) {
                removedActorArray.add(actor);
                finishedCars++;
            }
        }

        putActorsInGrid();

        for (Actor actor : actorArray) {
            if (detectCollision(actor)) {
                if (!(actor instanceof Obstacle)) {
                    removedActorArray.add(actor);
                    controlHistory.addAll(actor.controlHistory);
                }
                collisions++;

            }
        }

        // Some actors add new actors, so we have to use a buffer to avoid modifying the actor array while we're looping through it to step
        actorArray.addAll(newActorBuffer);
        newActorBuffer.clear();
        actorArray.removeAll(removedActorArray);
        carArray.removeAll(removedActorArray);
        pedestrianArray.removeAll(removedActorArray);
    }

    private ArrayList<Actor> getActorsFromGrid(Actor actor) {
        int x = (int) Math.floor(actor.x / (400. / grid));
        int y = (int) Math.floor(actor.y / (400. / grid));

        ArrayList<Actor> surroundingActors = new ArrayList<>();
        surroundingActors.addAll(actorGrid.get(x).get(y));
        surroundingActors.addAll(actorGrid.get(x - 1).get(y));
        surroundingActors.addAll(actorGrid.get(x).get(y - 1));
        surroundingActors.addAll(actorGrid.get(x - 1).get(y - 1));
        surroundingActors.addAll(actorGrid.get(x + 1).get(y));
        surroundingActors.addAll(actorGrid.get(x).get(y + 1));
        surroundingActors.addAll(actorGrid.get(x + 1).get(y + 1));
        surroundingActors.addAll(actorGrid.get(x - 1).get(y + 1));
        surroundingActors.addAll(actorGrid.get(x + 1).get(y - 1));

        return surroundingActors;
    }

    private void putActorsInGrid() {
        for (int i = -1; i < grid + 1; i++) {
            for (int j = -1; j < grid + 1; j++) {
                actorGrid.get(i).get(j).clear();
            }
        }
        for (Actor actor : actorArray) {
            int x = (int) Math.floor(actor.x / (400. / grid));
            int y = (int) Math.floor(actor.y / (400. / grid));
            actorGrid.get(x).get(y).add(actor);
        }
    }

    public boolean detectCollision(Actor actor) {
        for (Actor otherActor : actorArray) {
            // Collisions not dealing with cars are irrelevant
            if (otherActor instanceof Car || actor instanceof Car) {
                if (actor == otherActor)
                    continue;

                if (!actor.solid || !otherActor.solid)
                    continue;

                if (Utility.distance(actor.x, actor.y, otherActor.x, otherActor.y) < actor.radius + otherActor.radius)
                    return true;
            }
        }

        return false;
    }

    public void addActor(Actor actor) {
        newActorBuffer.add(actor);
    }

    public void addCar(Car car) {
        newActorBuffer.add(car);
        carArray.add(car);
    }

    public void addPedestrian(Pedestrian pedestrian) {
        newActorBuffer.add(pedestrian);
        pedestrianArray.add(pedestrian);
    }

    public void addObstacle(Obstacle obstacle) {
        newActorBuffer.add(obstacle);
        obstacleArray.add(obstacle);
    }

    public int getCollisions() {
        return collisions / 2;  // there are two actors involved in every collision so collisions ends up doubled, divide by two to get the true number of collisions
    }

    public int getFinishedCars() {
        return finishedCars;
    }

    public int getSpawnedCars() {
        return spawnedCars;
    }

    public ArrayList<Car> getCars() {
        return carArray;
    }

    public ArrayList<Pedestrian> getPedestrians() {
        return pedestrianArray;
    }

    public Random getRNG() {
        return rng;
    }

    public ArrayList<Obstacle> getObstacles() {
        return obstacleArray;
    }
}
