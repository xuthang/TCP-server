import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("starting\n");
        int port = 3999;

        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Couldn't open connection to port " + port);
            return;
        }

        while (true) {
            try (Socket clientSocket = serverSocket.accept()) {
                new Server(clientSocket).run();
            } catch (IOException e) {
                System.out.println("Couldn't create connection to port " + port);
                return;
            }

        }
    }
}
