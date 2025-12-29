import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * SimpleClient.java
 * A simple interactive client to connect to the Stock Management Server
 * Use this to manually test the server functionality
 */
public class SimpleClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        System.out.println("=== STOCK MANAGEMENT CLIENT ===");
        System.out.println("Connecting to server at " + SERVER_HOST + ":" + SERVER_PORT + "...\n");

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server!\n");

            // Start a thread to read server responses
            Thread readerThread = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    System.out.println("\nDisconnected from server.");
                }
            });
            readerThread.start();

            // Small delay to let initial messages come through
            Thread.sleep(500);

            // Read user input and send to server
            System.out.println("\nEnter commands (or type EXIT to quit):");
            String userInput;
            while (scanner.hasNextLine()) {
                userInput = scanner.nextLine();
                if (userInput.trim().isEmpty()) {
                    continue;
                }

                out.println(userInput);

                if (userInput.trim().equalsIgnoreCase("EXIT")) {
                    break;
                }

                // Small delay for better output formatting
                Thread.sleep(200);
            }

            readerThread.interrupt();

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.err.println("Make sure the server is running on port " + SERVER_PORT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

