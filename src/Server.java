import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Server implements Runnable {
    private Socket clientSocket;
    private Scanner in;
    private PrintWriter out;
    private String userInput;

    private Keys keys;
    private String username;
    //-----------------------------------
    static final String delim = "\u0007\u0008";

    static final String SERVER_KEY_REQUEST = "107 KEY REQUEST";
    static final String SERVER_OK = "200 OK";

    static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED";
    static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR";
    static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE";

    static final String SERVER_LOGOUT = "106 LOGOUT";

    private String SERVER_CONFIRMATION;

    static final String CLIENT_RECHARGING = "RECHARGING";
    static final String CLIENT_FULL_POWER = "FULL POWER";
    static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR";

    static final int TIMEOUT = 800; //1second
    static final int TIMEOUT_RECHARGING = 5000;//5seconds

    //-----------------------------------

    public Server(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.clientSocket.setSoTimeout(TIMEOUT);
        this.in = new Scanner(this.clientSocket.getInputStream()).useDelimiter(delim);
        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.keys = new Keys();
    }

    public static String toPrintable(String in) {
        return in.replace("\u0007", "\\a").replace("\u0008", "\\b");
    }

    private void send(String message) {
        message = message + delim;
        System.out.println("<-- sending |" + toPrintable(message) + "|");
        out.write(message);
        out.flush();
    }

    private String receive() throws TimeoutException, IllegalStateException, SocketException {
        userInput = read();

        if (userInput.equals(CLIENT_RECHARGING)) {
            this.clientSocket.setSoTimeout(TIMEOUT_RECHARGING);
            userInput = read();
            if (!userInput.equals(CLIENT_FULL_POWER))
                throw new IllegalStateException("robot is supposed to be full power");

            this.clientSocket.setSoTimeout(TIMEOUT);
            userInput = read();
        }


        return userInput;
    }

    private String read() throws TimeoutException {
        try {
            if (in.hasNext()) {
                String ret = in.next();
                System.out.println("received --> |" + toPrintable(ret) + "|");
                return ret;
            } else {
                throw new TimeoutException("no new message");
            }
        } catch (Throwable e) {
            throw new TimeoutException("no new message");
        }
    }

    @Override
    public void run() {
        System.out.println("new client connected...");

        try {
            if (!authenticate()) {
                System.out.println("authentication failed, ending connection...");
                return;
            }
        } catch (TimeoutException | SocketException e) {
            System.out.println("timed out, ending connection...");
            return;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            send(SERVER_LOGIC_ERROR);
            System.out.println("logic error, ending connection...");
            return;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            send(SERVER_SYNTAX_ERROR);
            System.out.println("syntax error, ending connection...");
            return;
        }


        RobotController controller = new RobotController(this::receive, this::send);

        try {
            controller.run();

        }  catch (TimeoutException | SocketException e) {
            System.out.println("timed out, ending connection...");
            return;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            send(SERVER_LOGIC_ERROR);
            System.out.println("logic error, ending connection...");
            return;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            send(SERVER_SYNTAX_ERROR);
            System.out.println("syntax error, ending connection...");
            return;
        }

        send(SERVER_LOGOUT);
    }

    private Boolean authenticate() throws TimeoutException, IllegalStateException, SocketException, NumberFormatException {
        receive();

        if (userInput.length() > 18) {
            send(SERVER_SYNTAX_ERROR);
            return false;
        }
        this.username = userInput;

        send(SERVER_KEY_REQUEST);

        receive();
        int ID = Integer.parseInt(userInput);
        if (ID >= keys.getSize() || ID < 0) {
            send(SERVER_KEY_OUT_OF_RANGE_ERROR);
            return false;
        }

        int hash = calcHash(username, keys.getServerKey(ID));
        SERVER_CONFIRMATION = Integer.toString(hash);
        send(SERVER_CONFIRMATION);

        receive();
        if (userInput.length() > 5) {
            throw new NumberFormatException("ID length > 5");
        }
        int result = Integer.parseInt(userInput);
        if (result != calcHash(username, keys.getClientKey(ID))) {
            send(SERVER_LOGIN_FAILED);
            return false;
        }

        send(SERVER_OK);

        return true;
    }

    private int calcHash(String string, int add) {
        int ret = 0;
        for (char ch : string.toCharArray())
            ret += ch;

        ret = (ret * 1000) % 65536;

        return (ret + add) % 65536;
    }

}
