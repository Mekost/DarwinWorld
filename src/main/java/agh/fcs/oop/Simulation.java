package agh.fcs.oop;

import agh.fcs.oop.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Simulation implements Runnable {
    private final World world;
    private ArrayList<Animal> animalList;
    private final int grassEnergy;
    private final int grassGrowth;
    private final int minEnergyForReproduction;
    private List<MapChangeListener> listeners = new ArrayList<>();
    private volatile boolean paused = false;

    @Override
    public void run() {
        while (!animalList.isEmpty()) {
            synchronized (this) {
                while (paused) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            try {
                List<Animal> toRemove = animalList.stream()
                        .filter(animal -> !animal.isAlive())
                        .toList();

                toRemove.forEach(animal -> {
                    world.removeAnimal(animal);
                    animalList.remove(animal);
                });

                notifySimulationStep("");

                for (Animal animal : animalList) {
                    world.move(animal);
                    if (world instanceof WorldPoles) {
                        if (animal.getPosition().x() >= world.getWidth() - 1 - ((WorldPoles) world).getPoleFields() / 2
                                || animal.getPosition().x() <= ((WorldPoles) world).getPoleFields() / 2) {
                            animal.setEnergy(animal.getEnergy() - 2);
                        } else if (animal.getPosition().x() >= world.getWidth() - 1 - ((WorldPoles) world).getPoleFields()
                                || animal.getPosition().x() <= ((WorldPoles) world).getPoleFields()) {
                            animal.setEnergy(animal.getEnergy() - 1);
                        }
                    }
                }

                notifySimulationStep("");

                Map<Vector2d, WorldElement> allElements = world.getElements();
                for (Animal animal : animalList) {
                    Vector2d position = animal.getPosition();
                    if (allElements.get(position) instanceof Grass) {
                        animal.setEnergy(animal.getEnergy() + grassEnergy);
                        world.removeGrass(position);
                    }
                }

                notifySimulationStep("");

                Map<Vector2d, List<Animal>> animalsGroupedByPosition = animalList.stream()
                        .filter(a -> a.getEnergy() > minEnergyForReproduction)
                        .collect(Collectors.groupingBy(Animal::getPosition));

                animalsGroupedByPosition.values().stream()
                        .map(a -> a.stream()
                                .sorted(Comparator.comparingInt(Animal::getEnergy).reversed()
                                        .thenComparingInt(Animal::getAge).reversed()
                                        .thenComparingInt(Animal::getChildrenNumber).reversed())
                                .limit(2)
                                .toList())
                        .filter(pair -> pair.size() == 2)
                        .forEach(pair -> {
                            Animal offspring = pair.get(0).reproduce(pair.get(1));
                            animalList.add(offspring);
                            try {
                                world.place(offspring);
                            } catch (IncorrectPositionException e) {
                                System.out.println("Incorrect position: " + offspring.getPosition());
                            }
                        });

                notifySimulationStep("");

                world.generatingGrass(grassGrowth);

                notifySimulationStep("");

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized void pause() {
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notify();
    }

    public boolean isPaused () {
       return paused;
    }

    public ArrayList<Animal> getAnimalList() {
        return animalList;
    }

    public Simulation(int width, int height, int startGrassCount, int startAnimalCount, int startAnimalEnergy,
                      int minReproductionEnergy, int energyUsedForReproduction, int minMutationCount,
                      int maxMutationCount, int grassEnergy, int grassGrowth, int geneLength) {
        this.world = new World(width, height, startGrassCount);
        ConsoleMapDisplay observer = new ConsoleMapDisplay();
        this.world.addObserver(observer);

        this.animalList = new ArrayList<>();
        AnimalConfig animalConfig = new AnimalConfig(startAnimalEnergy, energyUsedForReproduction, minMutationCount,
                maxMutationCount, geneLength);
        for (int i = 0; i < startAnimalCount; i++) {
            Vector2d position = new Vector2d(ThreadLocalRandom.current().nextInt(0, width - 1),
                    ThreadLocalRandom.current().nextInt(0, height - 1));
            animalList.add(new Animal(position, animalConfig));
        }

        for (Animal a : animalList) {
            try {
                world.place(a);
            } catch (IncorrectPositionException e) {
                System.out.println("Incorrect position: " + a.getPosition());
            }
        }

        this.grassEnergy = grassEnergy;
        this.grassGrowth = grassGrowth;
        this.minEnergyForReproduction = minReproductionEnergy;
    }

    public World getWorld() {
        return world;
    }

    public void addListener(MapChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(String message) {
        for (MapChangeListener listener : listeners) {
            listener.mapChanged(world, message);
        }
    }

    private void notifySimulationStep(String message) {
        for (MapChangeListener listener : listeners) {
            listener.mapChanged(world, message);
        }
    }
}
