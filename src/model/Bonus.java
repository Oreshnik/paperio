package model;

public class Bonus {
    public Point position;
    public Type type;
    public int time;

    public enum Type {
        SAW, NITRO, SLOW;

        static Type getTypeByCode(String s) {
            if (s.equals("saw")) {
                return SAW;
            } else if (s.equals("s")) {
                return SLOW;
            } else if (s.equals("n")) {
                return NITRO;
            }
            return null;
        }
    }
}
