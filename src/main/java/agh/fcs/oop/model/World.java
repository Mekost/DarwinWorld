package agh.fcs.oop.model;

import agh.fcs.oop.model.util.MapVisualizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World implements WorldMap {
    private final int width;
    private final int height;
    private final Vector2d bottomLeft;
    private final Vector2d upperRight;
    private final Vector2d equatorLeftCorner;
    private final Vector2d equatorRightCorner;
    private final int startingGrassAmount;
    private Map<Vector2d, Animal> animalMap = new HashMap<>();
    private Map<Vector2d, Grass> grassMap = new HashMap<>();
    private Map<Vector2d, WorldElement> allElements = new HashMap<>();
    private MapVisualizer visualizer = new MapVisualizer(this);
    private int mapID = this.hashCode();
    private List<MapChangeListener> observers = new ArrayList<>();

    public World(int width, int height, int startingGrassAmount) {
        this.width = width;
        this.height = height;
        this.bottomLeft = new Vector2d(0, 0);
        this.upperRight = new Vector2d(width - 1, height - 1);
//        double equatorHeight = Math.ceil(height * 1.0 /5);
//        TODO: some bugfixing with equators
        this.equatorLeftCorner = new Vector2d(0, (int)Math.ceil(0.4 * height));
        this.equatorRightCorner = new Vector2d(width - 1, (int)Math.floor(0.6 * height));
        this.startingGrassAmount = startingGrassAmount;
        int equatorStartingGrasses = (int)Math.ceil(startingGrassAmount * 0.8);
        int otherStartingGrasses = startingGrassAmount - equatorStartingGrasses;

        StartingGrassGenerator equatorPositionGenerator = new StartingGrassGenerator(equatorLeftCorner.x(), equatorLeftCorner.y(), equatorRightCorner.x(), equatorRightCorner.y(), equatorStartingGrasses);
        for (Vector2d grassPosition : equatorPositionGenerator) {
            grassMap.put(grassPosition, new Grass(grassPosition));
        }
        int upperOtherGrasses = (int) (Math.random() * otherStartingGrasses);
        int lowerOtherGrasses = otherStartingGrasses = upperOtherGrasses;

        StartingGrassGenerator upperPositionGenerator = new StartingGrassGenerator(0, equatorRightCorner.y() + 1, width - 1, height - 1, upperOtherGrasses);
        for (Vector2d grassPosition : upperPositionGenerator) {
            grassMap.put(grassPosition, new Grass(grassPosition));
        }

        StartingGrassGenerator lowerPositionGenerator = new StartingGrassGenerator(0, 0, width - 1, equatorLeftCorner.y(), lowerOtherGrasses);
        for (Vector2d grassPosition : lowerPositionGenerator) {
            grassMap.put(grassPosition, new Grass(grassPosition));
        }

    }

    public Map<Vector2d, Grass> getGrassMap() {
        return grassMap;
    }

    public List<Vector2d> preferredGrassPositions() {
        List<Vector2d> betterGrass = new ArrayList<>();
        for (int i = equatorLeftCorner.x(); i <= equatorRightCorner.x(); i++) {
            for (int j = 0; j <= width - 1; j++) {
                betterGrass.add(new Vector2d(i, j));
            }
        }
        return betterGrass;
    }

    public List<Vector2d> otherGrassPositions() {
        List<Vector2d> worseGrass = new ArrayList<>();
        for (int i = 0; i < equatorLeftCorner.y(); i++) {
            for (int j = 0; j <= width - 1; j++) {
                worseGrass.add(new Vector2d(i, j));
            }
        }
        for (int i = equatorRightCorner.y() + 1; i <= height - 1; i++) {
            for (int j = 0; j <= width - 1; j++) {
                worseGrass.add(new Vector2d(i, j));
            }
        }
        return worseGrass;
    }

    public void generatingGrasses(int grassGrowth) {
        int equatorGrasses = (int)Math.ceil(grassGrowth * 0.8);
        int tempGrassAmount = grassMap.size();

        GeneralGrassGenerator equatorPositionGenerator = new GeneralGrassGenerator(equatorLeftCorner.x(), equatorLeftCorner.y(), equatorRightCorner.x(), equatorRightCorner.y(), equatorGrasses, grassMap);
        for (Vector2d grassPosition : equatorPositionGenerator) {
            grassMap.put(grassPosition, new Grass(grassPosition));
        }

        int placedGrasses = grassMap.size() - tempGrassAmount;
        int otherGrasses = grassGrowth - placedGrasses;
//        System.out.println(otherGrasses);
        int upperOtherGrasses = (int)Math.round(Math.random() * otherGrasses);
//        System.out.println(upperOtherGrasses);
        tempGrassAmount = grassMap.size();

        GeneralGrassGenerator upperPositionGenerator = new GeneralGrassGenerator(0, equatorRightCorner.y() + 1, width - 1, height - 1, upperOtherGrasses, grassMap);
        for (Vector2d grassPosition : upperPositionGenerator) {
            grassMap.put(grassPosition, new Grass(grassPosition));
        }

        placedGrasses = grassMap.size() - tempGrassAmount;
        int lowerOtherGrasses = otherGrasses - placedGrasses;

        GeneralGrassGenerator lowerPositionGenerator = new GeneralGrassGenerator(0, 0, width - 1, equatorLeftCorner.y(), lowerOtherGrasses, grassMap);
        for (Vector2d grassPosition : lowerPositionGenerator) {
            grassMap.put(grassPosition, new Grass(grassPosition));
        }
    }

    public boolean canMoveTo(Vector2d position) {
        return position.follows(bottomLeft) && position.precedes(upperRight);
    }

    public boolean place(Animal animal) throws IncorrectPositionException {
        if (canMoveTo(animal.getPosition())) {
            animalMap.put(animal.getPosition(), animal);
            // notifyObservers
            return true;
        }
        else {
            throw new IncorrectPositionException(animal.getPosition());
        }
    }

    public void removeAnimal(Animal animal) {
        if (animal.getPosition() != null) {
            animalMap.remove(animal.getPosition(), animal);
        }
    }

    public void removeGrass(Vector2d position) {
        grassMap.remove(position);
    }

    public WorldElement objectAt(Vector2d position) {
        if (animalMap.get(position) != null) return animalMap.get(position);
        else if (grassMap.get(position) != null) return grassMap.get(position);
        return null;
    }

    @Override
    public void move(Animal animal) {
        Vector2d prevPosition = animal.getPosition();
        animal.move(this);
        animalMap.remove(prevPosition);
        animalMap.put(animal.getPosition(), animal);
    }

    public boolean isOccupied(Vector2d position) {
        return animalMap.containsKey(position);
    }


    public Map<Vector2d, WorldElement> getElements() {
        for (Map.Entry<Vector2d, Animal> element : animalMap.entrySet()) {
            allElements.put(element.getKey(), element.getValue());
        }

        for (Map.Entry<Vector2d, Grass> element : grassMap.entrySet()) {
            allElements.put(element.getKey(), element.getValue());
        }
        return allElements;
    }

    public int getID() {
        return mapID;
    }

}
