package model;

public enum Direction {
    UP(0, 1), RIGHT(1, 0), DOWN(0, -1), LEFT(-1, 0);
    public int dx, dy;
    public static Direction[] directions = {UP, RIGHT, DOWN, LEFT};
    private static Direction[] fromUp = {UP, RIGHT, LEFT};
    private static Direction[] fromRight = {UP, RIGHT, DOWN};
    private static Direction[] fromDown = {RIGHT, DOWN, LEFT};
    private static Direction[] fromLeft = {UP, DOWN, LEFT};

    Direction(int x, int y) {
        dx = x;
        dy = y;
    }

    public static Direction getDirection(String d) {
        return Direction.valueOf(d.toUpperCase());
    }

    public static Direction[] nextDirections(Direction direction) {
        if (direction == UP) {
            return fromUp;
        } else if (direction == RIGHT) {
            return fromRight;
        } else if  (direction == DOWN) {
            return fromDown;
        } else if (direction == LEFT) {
            return fromLeft;
        }
        return directions;
    }

    public Direction opposite() {
        if (this == UP) {
            return DOWN;
        } else if (this == RIGHT) {
            return LEFT;
        } else if  (this == DOWN) {
            return UP;
        } else {
            return LEFT;
        }
    }
}
