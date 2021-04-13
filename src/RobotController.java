import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

public class RobotController {
    static final String SERVER_MOVE = "102 MOVE";
    static final String SERVER_TURN_LEFT = "103 TURN LEFT";
    static final String SERVER_TURN_RIGHT = "104 TURN RIGHT";
    static final String SERVER_PICK_UP = "105 GET MESSAGE";

    ReadInput read;
    WriteOutput write;
    String curMessage;

    Coordinates curPos;
    DIRECTION curDir;

    public RobotController(ReadInput read, WriteOutput write) {
        this.read = read;
        this.write = write;
    }

    public void run() throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        figureOutStartPosition();
        goTo(new Coordinates(0, 0));
        pickUp();
    }

    void figureOutStartPosition() throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        //gets current position
        write.writeOuput(SERVER_TURN_LEFT);
        curMessage = read.readInput();
        curPos = getPosition(curMessage); //parses and checks the message

        //calculate what direction the robot is facing
        Coordinates nextPos = curPos;
        //try to move into any direction to calculate difference between current and next position
        for (int i = 0; i < 4; i++) {
            write.writeOuput(SERVER_TURN_LEFT);
            curMessage = read.readInput();
            Coordinates next = getPosition(curMessage); //parses to check for correctness

            write.writeOuput(SERVER_MOVE);
            curMessage = read.readInput();
            nextPos = getPosition(curMessage);

            if (!nextPos.equals(curPos))
                break;
        }

        if (nextPos.xPos > curPos.xPos) {
            curDir = DIRECTION.RIGHT;
        } else if (nextPos.xPos < curPos.xPos) {
            curDir = DIRECTION.LEFT;
        } else if (nextPos.yPos > curPos.yPos) {
            curDir = DIRECTION.UP;
        } else if (nextPos.yPos < curPos.yPos) {
            curDir = DIRECTION.DOWN;
        } else {
            throw new IllegalArgumentException("boxed robot, he cant move!!!");
        }
        curPos = nextPos;
    }

    Coordinates getPosition(String message) throws IllegalArgumentException {
        String[] split = message.split(" ");
        if(split.length < 2)
        {
            throw new IllegalArgumentException("not a CLIENT_OK message, got: |" + message + "|");
        }

        Coordinates ret = new Coordinates(Integer.parseInt(split[1]), Integer.parseInt(split[2])); //parsing can fail

        String expectation = "OK " + ret.xPos + " " + ret.yPos;
        if (!message.equals(expectation)) {
            throw new IllegalArgumentException("not a CLIENT_OK message, expected: |" + expectation + "| x got: |" + message + "|");
        }
        return ret;
    }

    private void goTo(Coordinates goal) throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        while (!curPos.equals(goal)) {
            if (curPos.xPos > goal.xPos) {
                if (!go(DIRECTION.LEFT)) {
                    // need to go left but immediate left is blocked
                    if (curPos.xPos - 1 == goal.xPos) {
                        if (curPos.yPos > goal.yPos) {
                            go(DIRECTION.DOWN);
                        } else {
                            go(DIRECTION.UP);
                        }
                        go(DIRECTION.LEFT);
                    } else {
                        go(DIRECTION.DOWN);
                        go(DIRECTION.LEFT);
                        go(DIRECTION.LEFT);
                        go(DIRECTION.UP);
                    }
                }
            } else if (curPos.xPos < goal.xPos) {
                if (!go(DIRECTION.RIGHT)) {
                    // need to go right but immediate right is blocked
                    if (curPos.xPos + 1 == goal.xPos) {
                        if (curPos.yPos > goal.yPos) {
                            go(DIRECTION.DOWN);
                        } else {
                            go(DIRECTION.UP);
                        }
                        go(DIRECTION.RIGHT);
                    } else {
                        go(DIRECTION.DOWN);
                        go(DIRECTION.RIGHT);
                        go(DIRECTION.RIGHT);
                        go(DIRECTION.UP);
                    }
                }
            } else if (curPos.yPos > goal.yPos) {
                if (!go(DIRECTION.DOWN)) //TRY TO GO DOWN
                {
                    if (curPos.yPos - 1 == goal.yPos) {
                        if (curPos.xPos > goal.xPos) {
                            go(DIRECTION.LEFT);
                        } else {
                            go(DIRECTION.RIGHT);
                        }
                        go(DIRECTION.DOWN); //GO DOWN BY 1 BUT ALSO 1 TO THE SIDE
                    } else { //GO DOWN BY 2
                        go(DIRECTION.LEFT);
                        go(DIRECTION.DOWN);
                        go(DIRECTION.DOWN);
                        go(DIRECTION.RIGHT);
                    }
                }
            } else if (curPos.yPos < goal.yPos) {
                if (!go(DIRECTION.UP)) //TRY TO GO UP BY 1
                {
                    if (curPos.yPos + 1 == goal.yPos) {
                        if (curPos.xPos > goal.xPos) {
                            go(DIRECTION.LEFT);
                        } else {
                            go(DIRECTION.RIGHT);
                        }
                        go(DIRECTION.UP); //UP BY 1 BUT THE OBSTACLE IS NOW ON THE LEFT OR THE RIGHT
                    } else { //GO UP BY 2
                        go(DIRECTION.LEFT);
                        go(DIRECTION.UP);
                        go(DIRECTION.UP);
                        go(DIRECTION.RIGHT);
                    }
                }
            }
        }
    }


    private Boolean go(DIRECTION dir) throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        while (dir != curDir) {
            spinLeft();
        }
        return goForwards();
    }

    private void spinLeft() throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        write.writeOuput(SERVER_TURN_LEFT);
        curMessage = read.readInput();
        Coordinates tmp = getPosition(curMessage);
        curDir = DIRECTION.nextDirection(curDir, DIRECTION.LEFT);
    }

    private Boolean goForwards() throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        write.writeOuput(SERVER_MOVE);
        curMessage = read.readInput();
        Coordinates nextPos = getPosition(curMessage);

        if (nextPos.equals(curPos))
            return false;

        curPos = nextPos;
        return true;
    }

    private void pickUp() throws TimeoutException, IllegalStateException, SocketException, IllegalArgumentException {
        write.writeOuput(SERVER_PICK_UP);
        curMessage = read.readInput();
        if (curMessage.length() > 98) {
            throw new IllegalArgumentException("message is too long");
        }
    }
}
