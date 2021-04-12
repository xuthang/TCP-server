import java.io.PrintWriter;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class RobotController {
    static final String SERVER_MOVE = "102 MOVE";
    static final String SERVER_TURN_LEFT = "103 TURN LEFT";
    static final String SERVER_TURN_RIGHT = "104 TURN RIGHT";
    static final String SERVER_PICK_UP = "105 GET MESSAGE";
    static final String SERVER_LOGOUT = "106 LOGOUT";

    static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR";
    static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR";

    static final String CLIENT_RECHARGING = "RECHARGING";
    static final String CLIENT_FULL_POWER = "FULL POWER";

    ReadInput read;
    WriteOutput write;
    Optional<String> curMessage;

    Coordinates curPos;
    DIRECTION curDir;

    public RobotController(ReadInput read, WriteOutput write) {
        this.read = read;
        this.write = write;
    }

    public Boolean run() {
        if (!figureOutStartPosition())
            return false;

        if (!goTo(new Coordinates(0, 0)))
            return false;

        return pickUp();
    }

    Boolean figureOutStartPosition() {

        //gets current position
        write.writeOuput(SERVER_TURN_LEFT);
        curMessage = read.readInput();
        if (curMessage.isEmpty())
            return false;
        curPos = getPosition(curMessage.get());

        //calculate what direction the robot is facing
        Coordinates nextPos = curPos;
        for (int i = 0; i < 4; i++) {
            write.writeOuput(SERVER_TURN_LEFT);
            curMessage = read.readInput();
            if (curMessage.isEmpty())
                return false;

            write.writeOuput(SERVER_MOVE);
            curMessage = read.readInput();
            if (curMessage.isEmpty())
                return false;
            nextPos = getPosition(curMessage.get());

            if (!nextPos.equals(curPos))
                break;
        }

        if (nextPos.xPos > curPos.xPos)
            curDir = DIRECTION.RIGHT;
        else if (nextPos.xPos < curPos.xPos)
            curDir = DIRECTION.LEFT;
        else if (nextPos.yPos > curPos.yPos)
            curDir = DIRECTION.UP;
        else if (nextPos.yPos < curPos.yPos)
            curDir = DIRECTION.DOWN;
        else
            return false;//boxed robot???

        curPos = nextPos;
        return true;
    }

    Coordinates getPosition(String message) {
        long spaces = message.chars().filter(ch -> ch == ' ').count();
        if(spaces > 2)
            throw new IllegalArgumentException("has too many spaces");

        String[] split = message.split(" ");
        return new Coordinates(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    private Boolean goTo(Coordinates goal) {
        while (!curPos.equals(goal)) {
            if (curPos.xPos > goal.xPos) {
                if (!go(DIRECTION.LEFT))
                    return false;
            } else if (curPos.xPos < goal.xPos) {
                if (!go(DIRECTION.RIGHT))
                    return false;
            } else if (curPos.yPos > goal.yPos) {
                if (!go(DIRECTION.DOWN))
                    return false;
            } else if (curPos.yPos < goal.yPos) {
                if (!go(DIRECTION.UP))
                    return false;
            }
        }
        return true;
    }

    private Boolean go(DIRECTION dir) {
        while (dir != curDir) {
            if (!spinLeft())
                return false;
        }
        return goForwards();
    }

    private Boolean spinLeft() {
        write.writeOuput(SERVER_TURN_LEFT);
        curMessage = read.readInput();
        if (curMessage.isEmpty())
            return false;

        curDir = DIRECTION.nextDirection(curDir, DIRECTION.LEFT);
        return true;
    }

    private Boolean goForwards() {
        write.writeOuput(SERVER_MOVE);
        curMessage = read.readInput();
        if (curMessage.isEmpty())
            return false;

        Coordinates nextPos = getPosition(curMessage.get());
        if (nextPos.equals(curPos))
            return false;

        curPos = nextPos;
        return true;
    }

    private Boolean pickUp() {
        write.writeOuput(SERVER_PICK_UP);
        curMessage = read.readInput();
        if (curMessage.isEmpty())
            return false;

        write.writeOuput(SERVER_LOGOUT);
        return true;
    }
}
