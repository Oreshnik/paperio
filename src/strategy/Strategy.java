package strategy;

import model.*;

import java.util.*;

import static model.Constants.*;

public class Strategy {
    private static List<Point> availableMoves = new ArrayList<>(3);
    private static List<Point> line = new ArrayList<>();
    private static Result best = new Result();
    private static HashMap<IdentityHashMap<Point, Integer>, Double> fillCash;

    public static String findMove(World world, Random random) {
        return randomSearch(world, random).name().toLowerCase();

    }

    private static Direction randomSearch(World world, Random random) {
        fillCash = new HashMap<>();
        long start = System.currentTimeMillis();
        prepareEnemies(world);
        double minEnemyLineDistance = getMinEnemyLineDistance(world.opponents, world.me.line.keySet());
        findSawDangerPoints(world);
        refreshBest(world, best, minEnemyLineDistance);

        int n = 0;
        int lastChange = 0;
        int timeLimit = Process.timeLeft / (world.remainingTicks + 20);

        while ((System.currentTimeMillis() - start < timeLimit || n < 100) && n - lastChange < CHANGE_CUT) {
            Result result = tryBranch(world, random, minEnemyLineDistance);
            if (result.score > best.score) {
                best = result;
                lastChange = n;
            }
            n++;
        }
        Process.timeLeft -= System.currentTimeMillis() - start;
        Process.debug += String.format("n: %d; lastChange: %d; cash: %d; score: %.2f; track: %d; med: %.2f; bfs: %s; timeLeft: %d, %s",
                n, lastChange, fillCash.size(), best.score, best.track.size(), minEnemyLineDistance, best.addSearch,
                Process.timeLeft / Math.max(1, world.remainingTicks), printTrack(world.me.position, best.track));

        return world.me.position.directionTo(best.track.get(0));
    }

    private static String printTrack(Point position, List<Point> track) {
        String res = "";
        Point point = position;
        for (Point next : track) {
            res += point.directionTo(next).name().substring(0, 1).toLowerCase();
            point = next;
        }
        return res;
    }

    private static List<Point> findPath(Player player, World world, Point target) {
        List<Point> result = new ArrayList<>();

        boolean[][] visited = new boolean[world.height][world.width];
        IdentityHashMap<Point, Point> edgeTo = new IdentityHashMap<>();
        Point edge = player.position;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(edge);
        visited[edge.y][edge.x] = true;

        Direction headDirection = player.direction;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            Point from = edgeTo.get(point);
            if (from != null) {
                headDirection = from.directionTo(point);
            }
            for (Direction direction : Direction.nextDirections(headDirection)) {
                Point next = point.move(direction);
                if (next == null || visited[next.y][next.x] || player.line.containsKey(next)) {
                    continue;
                }
                edgeTo.put(next, point);
                queue.add(next);
                visited[next.y][next.x] = true;
                if (target == next) {
                    do {
                        result.add(next);
                        next = edgeTo.get(next);
                    } while (next != null && next != edge);
                    Collections.reverse(result);
                    return result;
                }
            }
        }
        return result;
    }

    private static void prepareEnemies(World world) {
        for (Player player : world.opponents) {
            fillDistances(world, player);
            fillCaptures(world, player);
        }
    }

    private static void fillCaptures(World world, Player player) {
        for (Point point : player.territory.keySet()) {
            if (player.getDistance(point) > 10 || player.getDistance(point) == 0) {
                continue;
            }
            List<Point> path = findPath(player, world, point);
            if (path.size() < player.minCaptureTime) {
                player.minCaptureTime = path.size();
            }
            Point prevBase = path.size() == 1 ? player.position : path.get(path.size() - 2);
            if (!player.territory.containsKey(prevBase)) {
                //нужны только пути, конечная точка которых пришла с чужой территории
                player.paths.add(path);
                path.forEach(player::putLine);
                List<Bonus> bonuses = new ArrayList<>();
                calcFill(world, player, bonuses);
                if (path.size() <= 5 && bonuses.stream().anyMatch(b -> b.type == Bonus.Type.NITRO)) {
                    Bonus bonus = new Bonus();
                    bonus.type = Bonus.Type.NITRO;
                    bonus.time = 50;
                    player.bonuses.add(bonus);
                }
                player.captures.add(new IdentityHashMap<>(player.currentPlanedCapture));
                player.currentPlanedCapture.clear();
                fillCash.clear();
                path.forEach(p->player.line.remove(p));
            }
        }
    }

    private static void fillDistances(World world, Player player) {
        boolean[][] visited = new boolean[world.height][world.width];
        Point edge = player.position;
        IdentityHashMap<Point, Point> edgeTo = new IdentityHashMap<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        ArrayDeque<Point> nextQueue = new ArrayDeque<>();
        queue.add(edge);
        visited[edge.y][edge.x] = true;

        int distance = 0;

        Direction headDirection = player.direction;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            Point from = edgeTo.get(point);
            player.distances.put(point, distance);
            if (from != null) {
                headDirection = from.directionTo(point);
            }
            for (Direction direction : Direction.nextDirections(headDirection)) {
                Point next = point.move(direction);
                if (next == null || visited[next.y][next.x] || player.line.containsKey(next)) {
                    continue;
                }
                nextQueue.add(next);
                visited[next.y][next.x] = true;
                edgeTo.put(next, point);
            }
            if (queue.isEmpty()) {
                queue.addAll(nextQueue);
                nextQueue.clear();
                distance ++;
            }
        }
    }

    private static void refreshBest(World world, Result best, double minEnemyDistance) {
        if (best.track.size() < 3) {
            best.score = -Double.MAX_VALUE;
            best.track.clear();
            return;
        }

        best.track.remove(0);
        line.clear();
        Player me = world.me;
        Point start = me.position;
        Direction startDirection = me.direction;
        for (Point point : best.track) {
            if (!me.territory.containsKey(point)) {
                line.add(point);
                me.putLine(point);
            }
        }
        me.direction = best.track.get(best.track.size() - 2).directionTo(best.track.get(best.track.size() - 1));
        if (getAvailableMoves(world).size() > 0) {
            me.position = best.track.get(best.track.size() - 1);
        } else {
            me.position = null;
        }
        best.score = calcScore(world, best.track, minEnemyDistance);
        rollback(world, line, start, startDirection);
    }

    private static double getMinEnemyLineDistance(List<Player> opponents, Iterable<Point> line) {
        double minEnemyLineDistance = 500;
        for (Player enemy : opponents) {
            Bonus bonus = enemy.bonuses.stream().filter(b -> b.type != Bonus.Type.SAW).findAny().orElse(null);
            for (Point point : line) {
                double distance = getTimeToPoint(enemy, bonus, point);
                if (distance < minEnemyLineDistance) {
                    minEnemyLineDistance = distance;
                }
            }
        }

        return minEnemyLineDistance;
    }

    private static double getTimeToPoint(Player player, Bonus speedBonus, Point point) {
        double distance = (player.minCaptureTime <= 2 ? player.position.distanceTo(point) : player.getDistance(point))
                + player.addTime;
        distance = updateTimeWithBonus(speedBonus, distance);
        return distance;
    }

    private static double updateTimeWithBonus(Bonus speedBonus, double distance) {
        if (speedBonus != null) {
            double k = speedBonus.type == Bonus.Type.SLOW ? Constants.SLOW : Constants.NITRO;
            distance = Math.min(distance, speedBonus.time) * k + Math.max(0, distance - speedBonus.time);
        }
        return distance;
    }

    private static Result tryBranch(World world, Random random, double enemyDistance) {
        Result result = new Result();
        line.clear();
        Player me = world.me;
        me.currentPlanedCapture.clear();
        Point start = me.position;
        Direction startDirection = me.direction;
        int len = 0;
        boolean outOfTerritory = !me.territory.containsKey(me.position);

        double score = 0;
        while (len <= MAX_LEN && len < world.remainingTicks && me.position != null && (!me.territory.containsKey(me.position) || !outOfTerritory)) {
            Point nextInSameDir = me.direction == null ? null : me.position.move(me.direction);
            List<Point> moves = getAvailableMoves(world);
            Point move = null;
            if (moves.size() > 0) {

                //Если разрешено движение вперед, то с вероятностью 1/2 выбираем его
                if (nextInSameDir != null && !me.line.containsKey(nextInSameDir) && random.nextInt(2) < 1) {
                    move = nextInSameDir;
                } else {
                    //иначе выбираем случайное движение из всех возможных, в том числе и вперед
                    int index = random.nextInt(moves.size());
                    move = moves.get(index);
                }

                me.direction = me.position.directionTo(move);
                result.track.add(move);
                if (!me.territory.containsKey(move)) {
                    me.line.put(move, 1);
                    line.add(move);
                }
                if (!outOfTerritory && !me.territory.containsKey(move)) {
                    outOfTerritory = true;
                }
            }
            me.position = move;
            len++;
        }
        if (world.me.position != null && !world.me.territory.containsKey(world.me.position)) {
            List<Point> trackToBase = findShortestPathToBase(world, world.me, result.track.size());
            for (int i = 0; i < trackToBase.size(); i++) {
                Point next = trackToBase.get(i);
                if (!world.me.territory.containsKey(next)) {
                    world.me.putLine(next);
                    line.add(next);
                } else {
                    world.me.direction = i == 0 ? world.me.position.directionTo(next) : trackToBase.get(i - 1).directionTo(next);
                    world.me.position = next;
                }
                result.track.add(next);
            }
            result.addSearch = true;
        }

        score += calcScore(world, result.track, enemyDistance);

        rollback(world, line, start, startDirection);
        result.score = score;
        return result;
    }

    private static int outOfCapture(World world, Player player, IdentityHashMap<Point, Integer> capture, boolean toBase) {
        boolean[][] visited = new boolean[world.height][world.width];
        IdentityHashMap<Point, Point> edgeTo = new IdentityHashMap<>();
        Point edge = player.position;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(edge);
        visited[edge.y][edge.x] = true;

        Direction headDirection = player.direction;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            Point from = edgeTo.get(point);
            if (from != null) {
                headDirection = from.directionTo(point);
            }
            for (Direction direction : Direction.nextDirections(headDirection)) {
                Point next = point.move(direction);
                if (next == null || visited[next.y][next.x]) {
                    continue;
                }
                queue.add(next);
                visited[next.y][next.x] = true;
                edgeTo.put(next, point);

                if (!capture.containsKey(next) && (player.territory.containsKey(next) || !toBase)) {
                    int result = 0;
                    do {
                        result ++;
                        next = edgeTo.get(next);
                    } while (next != null && next != edge);
                    return result;
                }
            }
        }
        return 500;
    }

    private static List<Point> findShortestPathToBase(World world, Player player, int len) {
        List<Point> result = new ArrayList<>();

        boolean[][] visited = new boolean[world.height][world.width];
        IdentityHashMap<Point, Point> edgeTo = new IdentityHashMap<>();
        Point edge = player.position;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(edge);
        visited[edge.y][edge.x] = true;

        Direction headDirection = player.direction;
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            Point from = edgeTo.get(point);
            if (from != null) {
                headDirection = from.directionTo(point);
            }
            for (Direction direction : Direction.nextDirections(headDirection)) {
                Point next = point.move(direction);
                if (next == null || visited[next.y][next.x] || player.line.containsKey(next)) {
                    continue;
                }
                queue.add(next);
                visited[next.y][next.x] = true;
                edgeTo.put(next, point);

                if (player.territory.containsKey(next)) {
                    do {
                        result.add(next);
                        next = edgeTo.get(next);
                    } while (next != null && next != edge);
                    if (result.size() + len > world.remainingTicks) {
                        result.clear();
                        return result;
                    }
                    Collections.reverse(result);
                    return result;
                }
            }
        }
        return result;
    }

    private static double calcScore(World world, List<Point> track, double enemyDistance) {
        double score = 0;
        List<Bonus> bonuses = new ArrayList<>();
        if (world.me.position == null) {
            //некуда ходить
            score = STACK_BONUS;

        } else if (!world.me.territory.containsKey(world.me.position)) {
            //не замкнули
            score = scoreDistanceToBase(world);

        } else if (world.me.line.isEmpty()) {
            //не вышли из территории
            /*int shortest = track.isEmpty() ? 0 : track.get(0).distanceTo(track.get(track.size() - 1));
            if (shortest + 1 < track.size()) {
                //болтались без толку по территории
                score = -100 * (track.size() - (shortest + 1));
            }*/
            score += world.influenceMap[track.get(0).y][track.get(0).x] / 100;

        } else {

            double fill = calcFill(world, world.me, bonuses);
            score = fill * fill / Math.max(track.size(), world.me.line.size()) /*/ Math.max(1, (world.me.line.size() - NON_FINED_LEN) / LINE_FINE_DIVIDER)*/
                    + calcLineMaxInfluenceBonus(world);
        }

        for (Bonus bonus : world.bonuses.values()) {
            if (track.contains(bonus.position)) {
                bonuses.add(bonus);
            }
        }
        score += calcEnemyCollision(world, track) + calcBonuses(bonuses) + calcEnemyCapture(world, track)
                + (track.isEmpty() ? 0 : calcSawDanger(world, track.get(0)));
        enemyDistance = Math.min(enemyDistance,
                getMinEnemyLineDistance(world.opponents, line));

        int len = track.size();
        Bonus myBonus = world.me.bonuses.stream().filter(b -> b.type != Bonus.Type.SAW).findAny().orElse(null);
        if (myBonus != null) {
            double k = myBonus.type == Bonus.Type.SLOW ? Constants.SLOW : Constants.NITRO;
            len = (int) Math.ceil(Math.min(track.size(), myBonus.time) * k) + Math.max(0, track.size() - myBonus.time);
        }

        if (len + TAIL_BUFFER > enemyDistance) {
            if (score > 0) {
                score /= 100;
            }
            if (!world.me.territory.containsKey(world.me.position)) {
                score += STACK_BONUS;
            } else {
                score -= (len + TAIL_BUFFER - enemyDistance) * FILL_MULTI * FILL_OTHER_BONUS * 10;
            }
        }

        return score;
    }

    private static double calcSawDanger(World world, Point next) {
        double score = 0;
        for (Map.Entry<Point, Direction> entry : world.sawDanger.entrySet()) {
            if (Direction.UP == entry.getValue() && entry.getKey().x == next.x && entry.getKey().y < next.y
                    || Direction.DOWN == entry.getValue() && entry.getKey().x == next.x && entry.getKey().y > next.y
                    || Direction.RIGHT == entry.getValue() && entry.getKey().y == next.y && entry.getKey().x < next.x
                    || Direction.LEFT == entry.getValue() && entry.getKey().y == next.y && entry.getKey().x > next.x) {
                score += STACK_BONUS / 10;
            }
        }
        return score;
    }

    private static void findSawDangerPoints(World world) {
        for (Point point : world.bonuses.keySet()) {
            Bonus bonus = world.bonuses.get(point);
            if (bonus.type != Bonus.Type.SAW) {
                continue;
            }

            for (Player player : world.opponents) {
                if (player.position.distanceTo(point) == 1 && player.direction != point.directionTo(player.position)) {
                    world.sawDanger.put(point, player.position.directionTo(point));
                }
            }
        }
    }

    private static double calcEnemyCapture(World world, List<Point> track) {
        double min = 0;
        for (Player player : world.opponents) {
            for (int i = 0; i < player.paths.size(); i++) {
                List<Point> path = player.paths.get(i);
                IdentityHashMap<Point, Integer> capture = player.captures.get(i);

                if (track.size() >= path.size() && capture.containsKey(track.get(path.size() - 1))) {
                    //окружили голову, нам конец
                    min = Math.min(STACK_BONUS / path.size(), min);
                }

                if (track.size() >= path.size() && capture.containsKey(world.me.position)) {
                    //отъел базу, куда мы собирались
                    min = Math.min(min, STACK_BONUS / 3 / path.size());
                }
                //теперь ситуация, когда мы зашли на территорию до окружения
                if (capture.containsKey(world.me.position) && !world.me.currentPlanedCapture.containsKey(path.get(path.size() - 1))) {
                    //и вся территория окружена
                    if (world.me.currentPlanedCapture.keySet().stream().allMatch(capture::containsKey)) {
                        //и не успеваем с нее уйти
                        if (track.size() + outOfCapture(world, world.me, capture, true) >= path.size()) {
                            min = Math.min(STACK_BONUS / path.size(), min);
                        }
                    }
                }
            }
        }
        return min;
    }

    private static double scoreDistanceToBase(World world) {
        if (world.me.territory.size() == 0) {
            return world.influenceMap[world.me.position.y][world.me.position.x] / world.me.line.size();
        }
        int minDistance = 100500;
        for (Point point : world.me.territory.keySet()) {
            int distance = point.distanceTo(world.me.position);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return 1. / minDistance;
    }

    private static double calcBonuses(List<Bonus> bonuses) {
        double score = 0;
        for (Bonus bonus : bonuses) {
            if (bonus.type == Bonus.Type.SLOW) {
                score -= 500;
            } else {
                score += 25;
            }
        }
        return score;
    }

    private static double calcEnemyCollision(World world, List<Point> track) {
        for (Player player : world.opponents) {
            if (world.me.startPosition.distanceTo(player.position) == 1 && player.line.size() <= world.me.line.size()) {
                Direction directionToEnemy = world.me.startPosition.directionTo(player.position);
                Direction firstDirection = world.me.startPosition.directionTo(track.get(0));

                if (directionToEnemy == firstDirection) {
                    return STACK_BONUS;

                } else if (directionToEnemy.opposite() != firstDirection) {
                    return STACK_BONUS / 10;
                }
            }
            double trackTime = updateTimeWithBonus(world.me.speedBonus, track.size());
            //Для отсечения проверим простой дистанцией, округление кверху, потому что даже пересечение кусочком убивает
            if (Math.floor(updateTimeWithBonus(player.speedBonus, player.getDistance(world.me.position)+ player.addTime)) <= trackTime) {
                List<Point> pathToEnd = findPath(player, world, world.me.position);
                if (updateTimeWithBonus(player.speedBonus, pathToEnd.size()) <= trackTime
                        && getLineSizeTo(pathToEnd, player) <= world.me.line.size() + line.size()) {
                    return STACK_BONUS / 10;
                }
            }

            if (track.size() > 1) {
                if (Math.floor(updateTimeWithBonus(player.speedBonus, player.getDistance(track.get(track.size() - 2)) + player.addTime)) <= trackTime) {
                    List<Point> pathToPreEnd = findPath(player, world, track.get(track.size() - 2));

                    if (updateTimeWithBonus(player.speedBonus, pathToPreEnd.size()) <= trackTime
                            && getLineSizeTo(pathToPreEnd, player) <= world.me.line.size() + line.size()) {
                        return STACK_BONUS / 10;
                    }
                }
            }
        }
        return 0;
    }

    private static int getLineSizeTo(List<Point> path, Player player) {

        int len = 0;
        boolean terr = false;
        if (path.size() > 1) {
            for (int i = path.size() - 2; i >= 0; i--) {
                Point point = path.get(i);
                if (player.territory.containsKey(point)) {
                    terr = true;
                    break;
                }
                len ++;
            }
        }
        if (!terr) {
            len += player.line.size();
        }
        return len;
    }

    private static double calcLineMaxInfluenceBonus(World world) {
        double score = 0;
        for (Point point : world.me.line.keySet()) {
            score = Math.max(world.influenceMap[point.y][point.x], score);
        }
        return score;
    }

    static double calcFill(World world, Player player, List<Bonus> catchBonuses) {
        Double cashed = fillCash.get(player.line);
        if (cashed != null) {
            return cashed;
        }

        boolean[][] visited = new boolean[world.height][world.width];
        //fill all other area
        fillAllOtherArea(player, visited);

        //Посчитаем захват
        double area = fillMyArea(world, player, visited, catchBonuses);
        if (world.me == player) {
            fillCash.put(new IdentityHashMap<>(player.line), area);
        }
        return area;
    }

    private static double fillMyArea(World world, Player player, boolean[][] visited, List<Bonus> catchBonuses) {
        double score = 0;
        ArrayDeque<Point> queue = new ArrayDeque<>();
        for (Point terr : player.line.keySet()) {
            if (visited[terr.y][terr.x]) {
                continue;
            }
            queue.add(terr);
            visited[queue.getFirst().y][queue.getFirst().x] = true;

            while (!queue.isEmpty()) {
                Point point = queue.removeFirst();

                if (!player.territory.containsKey(point)) {
                    player.currentPlanedCapture.put(point, 1);
                }

                if (player == world.me) {
                    if (world.opponentsTerritory.containsKey(point)) {
                        score += Constants.FILL_OTHER_BONUS;

                    } else if (!player.territory.containsKey(point)) {
                        score += Constants.FILL_BONUS;
                    }
                }

                if (world.bonuses.containsKey(point) && !player.territory.containsKey(point) && !player.line.containsKey(point)) {
                    catchBonuses.add(world.bonuses.get(point));
                }

                for (Direction direction : Direction.directions) {
                    Point next = point.move(direction);
                    if (next == null || visited[next.y][next.x] || player.territory.containsKey(next)) {
                        continue;
                    }
                    queue.add(next);
                    visited[next.y][next.x] = true;
                }
            }
        }

        return score;
    }

    private static void fillAllOtherArea(Player player, boolean[][] visited) {
        for (Point edge : Point.edges) {
            if (visited[edge.y][edge.x] || player.territory.containsKey(edge) || player.line.containsKey(edge)) {
                continue;
            }

            ArrayDeque<Point> queue = new ArrayDeque<>();
            queue.add(edge);
            visited[edge.y][edge.x] = true;
            while (!queue.isEmpty()) {
                Point point = queue.removeFirst();
                for (Direction direction : Direction.directions) {
                    Point next = point.move(direction);
                    if (next == null || visited[next.y][next.x] || player.line.containsKey(next) || player.territory.containsKey(next)) {
                        continue;
                    }
                    queue.add(next);
                    visited[next.y][next.x] = true;
                }
            }
        }
    }

    private static void rollback(World world, List<Point> line, Point start, Direction startDirection) {
        for (int i = 0; i < line.size(); i++) {
            world.me.line.remove(line.get(i));
        }
        world.me.position = start;
        world.me.direction = startDirection;
    }


    public static List<Point> getAvailableMoves(World world) {
        availableMoves.clear();
        Player me = world.me;
        Direction[] directions = Direction.nextDirections(me.direction);
        for (Direction direction : directions) {
            Point point = me.position.move(direction);
            if (point != null && !me.line.containsKey(point)) {
                availableMoves.add(point);
            }
        }
        return availableMoves;
    }

    public static class Result {
        List<Point> track = new ArrayList<>();
        double score;
        boolean addSearch = false;
    }
}
