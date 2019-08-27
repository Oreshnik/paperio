package strategy;

import model.Constants;
import model.World;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import static model.Constants.TEST_TIME_LIMIT;
import static model.Constants.TIME_LIMIT;

public class Process {
    public static boolean testEnv;
    private static Random random = new Random(100500);
    public static String debug;
    public static int timeLeft = (testEnv ? TEST_TIME_LIMIT : TIME_LIMIT) * (Constants.LAST_TICK_NUM + 1) / 6;

    public static void process() throws IOException {
        FileWriter writer = null;
        if ("true".equals(System.getenv("TEST_ENV"))) {
            testEnv = true;
            writer = new FileWriter("C:\\Documents\\Projects\\RAIC\\Paperio\\gameLog.txt");
        }
        JSONObject gameMessage;
        World world = new World();

        while ((gameMessage = JsonIO.readFromStdIn()) != null) {
            if (writer != null) {
                writer.write(gameMessage + "\n");
            }
            try {
                debug = "";
                MessageType messageType = gameMessage.getEnum(MessageType.class, "type");
                switch (messageType) {
                    case tick:
                        world.initTick(gameMessage.getJSONObject("params"));
                        String command = Strategy.findMove(world, random);
                        if (writer != null) {
                            writer.write(command + "\n");
                        }
                        System.out.printf("{\"command\": \"%s\", \"debug\": \"%s\"}\n", command, debug + " " + command);
                        break;

                    case start_game:

                        world.init(gameMessage.getJSONObject("params"));
                        break;
                    case end_game:
                        break;
                }

            }
            catch (Exception e) {
                if (writer != null) {
                    writer.write(e.toString() + " " + e.getMessage());
                    writer.flush();
                    writer.close();
                }
                throw e;
            }
        }
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

    enum MessageType {
        start_game,
        tick,
        end_game
    }
}
