package agh.fcs.oop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulationTest {

    @Test
    public void removeDeadAnimalsTest() {
        Simulation simulation = new Simulation("No variant", "Random gene", 5, 5, 0, 5, 2, 5, 5, 1, 5, 3, 0, 1, 100, false);
        simulation.run();

        assertEquals(0, simulation.getAnimalList().size());
    }
}
