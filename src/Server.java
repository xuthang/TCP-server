import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
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

    private String SERVER_CONFIRMATION;
    static final String SERVER_MOVE = "102 MOVE" + delim;
    static final String SERVER_TURN_LEFT = "103 TURN LEFT" + delim;
    static final String SERVER_TURN_RIGHT = "104 TURN RIGHT" + delim;
    static final String SERVER_PICK_UP = "105 GET MESSAGE" + delim;
    static final String SERVER_LOGOUT = "106 LOGOUT" + delim;
    static final String SERVER_KEY_REQUEST = "107 KEY REQUEST" + delim;
    static final String SERVER_OK = "200 OK" + delim;
    static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED" + delim;
    static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR" + delim;
    static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR" + delim;
    static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE" + delim;

    static final String CLIENT_RECHARGING = "RECHARGING" + delim;
    static final String CLIENT_FULL_POWER = "FULL POWER" + delim;

    //-----------------------------------

    public Server(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        //this.clientSocket.setSoTimeout(500);
        this.in = new Scanner(this.clientSocket.getInputStream()).useDelimiter(delim);
        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.keys = new Keys();
    }

    public static String toPrintable(String in) {
        return in.replace("\u0007", "\\a").replace("\u0008", "\\b");
    }

    private void send(String message) {
        System.out.println("<-- |" + toPrintable(message) + "|");
        out.write(message);
        out.flush();
    }

    private Optional<String> receive() {
        if (!in.hasNext()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return userInput = Optional.empty();
            }
        }

        if (!in.hasNext())
            return userInput = Optional.empty();

        userInput = Optional.ofNullable(in.next());
        if (!userInput.isEmpty())
            System.out.println("--> |" + toPrintable(userInput.get()) + "|");

        return userInput;
    }

    @Override
    public void run() {
        System.out.println("new client connected...");

        if (!authenticate()) {
            System.out.println("ending connection...");
            return;
        }

    }

    private Boolean authenticate() {
        receive();
        if (userInput.isEmpty() || userInput.get().length() > 18) {
            return false;
        }
        this.username = userInput.get();

        send(SERVER_KEY_REQUEST);

        if (receive().isEmpty()) {
            return false;
        }
        int ID = Integer.parseInt(userInput.get());

        int hash = calcHash(username, keys.getServerKey(ID));
        SERVER_CONFIRMATION = Integer.toString(hash) + delim;
        send(SERVER_CONFIRMATION);

        if (receive().isEmpty()) {
            return false;
        }

        hash = calcHash(username, keys.getClientKey(ID));
        if(Integer.parseInt(userInput.get()) != hash)
        {
            send(SERVER_LOGIN_FAILED);
            return false;
        }

        send(SERVER_OK);

        return false;
    }

    private int calcHash(String string, int add)
    {
        int ret = 0;
        for (char ch: string.toCharArray())
            ret += (int)ch;

        ret = (ret * 1000) % 65536;

        return (ret + add)% 65536;
    }
}
