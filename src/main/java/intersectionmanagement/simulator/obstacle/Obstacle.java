package intersectionmanagement.simulator.obstacle;

import intersectionmanagement.simulator.Actor;
import intersectionmanagement.simulator.Simulator;
import intersectionmanagement.simulator.Utility;
import intersectionmanagement.simulator.track.Node;

import java.util.ArrayList;

public class Obstacle extends Actor {
    boolean active;

    public Obstacle(Simulator simulator, Node firstTarget, float x, float y, float radius) {
        super(simulator, firstTarget);
        this.x = x;
        this.y = y;
        speed = 0;
        this.radius = radius;
        solid = true;
        active = false;
    }


    @Override
    public void step(ArrayList<Actor> actorArray) {
        if (!active) {
            simulator.addObstacle(this);
            active = true;
        }
    }

    void moveTowardsTarget() {
        return;
    }
}
