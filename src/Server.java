import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

public class Server implements Runnable {
    private Socket clientSocket;
    private Scanner in;
    private PrintWriter out;
    private Optional<String> userInput;

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
        System.out.println("<-- |" + toPrintable(message) + "|");
        out.write(message);
        out.flush();
    }

    private Optional<String> receive() {
        String input;
        try {
            if (in.hasNext()) {
                input = in.next();
            } else {
                input = null;
            }
        } catch (Throwable e) {
            return Optional.empty();
        }

        userInput = Optional.ofNullable(input);
        if (userInput.isPresent())
            System.out.println("--> |" + toPrintable(userInput.get()) + "|");
        else
            System.out.println("--> didn't receive anything");

        return userInput;
    }

    @Override
    public void run() {
        System.out.println("new client connected...");

        if (!authenticate()) {
            System.out.println("ending connection...");
            return;
        }

        RobotController controller = new RobotController(this::receive, this::send);

        try {
            if (!controller.run()) {
                System.out.println("ending connection...");
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace();

            send(SERVER_SYNTAX_ERROR);
            return;
        }

        send(SERVER_LOGOUT);
    }

    private Boolean authenticate() {
        if (receive().isEmpty()) {
            return false;
        }
        if (userInput.get().length() > 18) {
            send(SERVER_SYNTAX_ERROR);
            return false;
        }
        this.username = userInput.get();

        send(SERVER_KEY_REQUEST);

        if (receive().isEmpty()) {
            return false;
        }
        int ID;
        try {
            ID = Integer.parseInt(userInput.get());
        } catch (final NumberFormatException e) {
            send(SERVER_SYNTAX_ERROR);
            return false;
        }
        if (ID >= keys.getSize() || ID < 0) {
            send(SERVER_KEY_OUT_OF_RANGE_ERROR);
            return false;
        }

        int hash = calcHash(username, keys.getServerKey(ID));
        SERVER_CONFIRMATION = Integer.toString(hash);
        send(SERVER_CONFIRMATION);

        if (receive().isEmpty()) {
            return false;
        }

        if (userInput.get().length() > 5) {
            send(SERVER_SYNTAX_ERROR);
            return false;
        }

        int result;
        try {
            result = Integer.parseInt(userInput.get());
        } catch (final NumberFormatException e) {
            send(SERVER_SYNTAX_ERROR);
            return false;
        }

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
