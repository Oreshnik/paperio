package model;

import java.util.ArrayList;
import java.util.List;

public class Point {
    public final int x, y;
    private static Point[][] points;
    private static int size;
    public static List<Point> edges = new ArrayList<>();

    private Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static void init(int width, int height, int cellSize) {
        points = new Point[height][width];
        size = cellSize;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                points[i][j] = new Point(j, i);
            }
            edges.add(Point.get(0, i));
            edges.add(Point.get(width - 1, i));
        }
        for (int i = 1; i < width - 1; i ++) {
            edges.add(Point.get(i, 0));
            edges.add(Point.get(i, height - 1));
        }
    }

    public static Point get(int x, int y) {
        if (x < 0 || y < 0 || x >= points[0].length || y >= points.length) {
            return null;
        }
        return points[y][x];
    }

    static Point convert(int x, int y) {
        return get((int) Math.round((x - size / 2.0) / size), (int) Math.round((y - size / 2.0) / size));
    }

    public Point move(Direction direction) {
        return get(this.x + direction.dx, this.y + direction.dy);
    }

    public Direction directionTo(Point next) {
        for (Direction direction : Direction.directions) {
            if (move(direction) == next) {
                return direction;
            }
        }
        return null;
    }

    public int distanceTo(Point other) {
        return Math.abs(other.x - x) + Math.abs(other.y - y);
    }

    @Override
    public String toString() {
        return x + " " + y;
    }
}
