package model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class World {
    public Player me;
    public List<Player> opponents;
    public IdentityHashMap<Point, Integer> opponentsTerritory;
    public int width, height, speed;
    public int tick;
    public int remainingTicks;
    public IdentityHashMap<Point, Bonus> bonuses;
    public double[][] influenceMap;
    public Map<Point, Direction> sawDanger;
    private int cellWidth;

    public World() {
    }

    public void init(JSONObject params) {
        cellWidth = params.getInt("width");
        width = params.getInt("x_cells_count");
        height = params.getInt("y_cells_count");
        Point.init(width, height, cellWidth);
    }

    public void initTick(JSONObject params) {
        tick = params.getInt("tick_num");
        remainingTicks = Math.max(1, (Constants.LAST_TICK_NUM - tick) / 6);
        if (remainingTicks < 0) {
            remainingTicks = 100500;
        }
        opponents = new ArrayList<>();
        opponentsTerritory = new IdentityHashMap<>();
        for (Object id : params.getJSONObject("players").names()) {
            JSONObject p = params.getJSONObject("players").getJSONObject((String) id);

            Direction direction = null;
            if (!p.get("direction").toString().equals("null")) {
                direction = Direction.valueOf(p.getString("direction").toUpperCase());
            }

            JSONArray pos = p.getJSONArray("position");
            int x, y;
            double addTime = 0;
            if (direction == Direction.UP) {
                x = (pos.getInt(0) - cellWidth / 2) / cellWidth;
                y = (int) Math.ceil((pos.getInt(1) - cellWidth / 2.0) / cellWidth);
                addTime = ((cellWidth - (pos.getInt(1) - cellWidth / 2) % cellWidth) * 1.0 / cellWidth) % 1;

            } else if (direction == Direction.DOWN) {
                x = (pos.getInt(0) - cellWidth / 2) / cellWidth;
                y = (int) Math.floor((pos.getInt(1) - cellWidth / 2.0) / cellWidth);
                addTime = (((pos.getInt(1) - cellWidth / 2) % cellWidth) * 1.0 / cellWidth) % 1;

            } else if (direction == Direction.LEFT) {
                x = (int) Math.floor((pos.getInt(0) - cellWidth / 2.) / cellWidth);
                y = (pos.getInt(1) - cellWidth / 2) / cellWidth;
                addTime = (((pos.getInt(0) - cellWidth / 2) % cellWidth) * 1.0 / cellWidth) % 1;

            } else if (direction == Direction.RIGHT) {
                x = (int) Math.ceil((pos.getInt(0) - cellWidth / 2.) / cellWidth);
                y = (pos.getInt(1) - cellWidth / 2) / cellWidth;
                addTime = ((cellWidth - (pos.getInt(0) - cellWidth / 2) % cellWidth) * 1.0 / cellWidth) % 1;

            } else {
                x = (pos.getInt(0) - cellWidth / 2) / cellWidth;
                y = (pos.getInt(1) - cellWidth / 2) / cellWidth;
            }
            Player player = new Player(Point.get(x, y));
            player.addTime = addTime;

            player.score = p.getInt("score");
            JSONArray lines = p.getJSONArray("lines");
            for (Object c : lines) {
                JSONArray line = (JSONArray) c;
                player.putLine(Point.convert(line.getInt(0), line.getInt(1)));
            }

            JSONArray terr = p.getJSONArray("territory");
            for (Object c : terr) {
                JSONArray t = (JSONArray) c;
                player.putTerritory(Point.convert(t.getInt(0), t.getInt(1)));
            }

            JSONArray bonuses = p.getJSONArray("bonuses");
            for (Object c : bonuses) {
                JSONObject b = (JSONObject) c;
                Bonus bonus = new Bonus();
                bonus.time = b.getInt("ticks");
                bonus.type = Bonus.Type.getTypeByCode(b.getString("type"));
                player.bonuses.add(bonus);

                if (bonus.type == Bonus.Type.SLOW || bonus.type == Bonus.Type.NITRO) {
                    player.speedBonus = bonus;
                }
            }

            if (!p.get("direction").toString().equals("null")) {
                player.direction = Direction.valueOf(p.getString("direction").toUpperCase());
            }
            if (id.equals("i")) {
                me = player;
            } else {
                opponents.add(player);
                opponentsTerritory.putAll(player.territory);
            }
        }
        bonuses = new IdentityHashMap<>();
        for (Object o : params.getJSONArray("bonuses")) {
            JSONObject jsonObject = (JSONObject) o;
            Bonus bonus = new Bonus();
            bonus.position = Point.convert(jsonObject.getJSONArray("position").getInt(0), jsonObject.getJSONArray("position").getInt(1));
            bonus.type = Bonus.Type.getTypeByCode(jsonObject.getString("type"));
            bonuses.put(bonus.position, bonus);
        }


        initInfluence();
        sawDanger = new HashMap<>();
    }

    private void initInfluence() {
        influenceMap = new double[height][width];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int dx = 0; dx < width; dx++) {
                    for (int dy = 0; dy < height; dy++) {
                        Point point = Point.get(dx, dy);
                        int len = Math.abs(x - dx) + Math.abs(y + dy);
                        if (len == 0 || point == null || len <= 3 || me.territory.containsKey(point)) {
                            continue;
                        }

                        if (opponentsTerritory.containsKey(point)) {
                            influenceMap[y][x] += 2.5 / len;
                        } else {
                            influenceMap[y][x] += 0.5 / len;
                        }
                    }
                }
                for (Player enemy : opponents) {
                    influenceMap[y][x] -= 31 / Math.max(1, enemy.position.distanceTo(Point.get(x, y)));
                }
            }
        }
    }
}
