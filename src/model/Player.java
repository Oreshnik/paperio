package model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class Player {
    private static Integer muff = 1;

    public Point position;
    public final Point startPosition;
    public int score, id;
    public Direction direction;
    public IdentityHashMap<Point, Integer> territory;
    public IdentityHashMap<Point, Integer> line;
    public boolean alive;
    public List<Bonus> bonuses;
    public Bonus speedBonus;
    public List<List<Point>> paths;
    public IdentityHashMap<Point, Integer> currentPlanedCapture;
    public List<IdentityHashMap<Point, Integer>> captures;
    public IdentityHashMap<Point, Integer> distances;
    public int minCaptureTime;
    public double addTime;

    public Player(Point position) {
        territory = new IdentityHashMap<>();
        alive = true;
        line = new IdentityHashMap<>();
        this.position = position;
        startPosition = position;
        bonuses = new ArrayList<>();
        paths = new ArrayList<>();
        currentPlanedCapture = new IdentityHashMap<>();
        captures = new ArrayList<>();
        distances = new IdentityHashMap<>();
        minCaptureTime = 500;
        speedBonus = null;
    }

    public void putLine(Point position) {
        line.put(position, muff);
    }

    public void putTerritory(Point position) {
        territory.put(position, muff);
    }

    public int getDistance(Point point) {
        return distances.getOrDefault(point, 500);
    }
}
