import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
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
            Socket clientSocket;
            try  {
                clientSocket = serverSocket.accept();
                long threadId = Thread.currentThread().getId();
                System.out.println("main using " + threadId);
                Server runnable = new Server(clientSocket);
                Thread thread = new Thread(runnable);
                thread.start();
            } catch (IOException e) {
                System.out.println("Couldn't create connection to port " + port);
                return;
            }

        }
    }
}
