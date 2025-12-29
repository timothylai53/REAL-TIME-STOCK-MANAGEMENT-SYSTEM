import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server.java
 * Main server class for the Real-Time Stock Management System
 *
 * Requirement A (Threads/Runnable): This server creates a new Thread for each client
 * connection. Each ClientHandler runs on its own thread to handle concurrent requests.
 */
public class Server {
    private static final int PORT = 8888;
    private final InventoryManager inventoryManager;
    private final AtomicInteger clientCounter;
    private volatile boolean isRunning;

    public Server() {
        this.inventoryManager = new InventoryManager();
        this.clientCounter = new AtomicInteger(0);
        this.isRunning = true;
    }

    /**
     * Start the server and listen for incoming connections
     */
    public void start() {
        System.out.println("=== REAL-TIME STOCK MANAGEMENT SERVER ===");
        System.out.println("Server starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and waiting for connections...");
            System.out.println("Press Ctrl+C to stop the server.\n");

            while (isRunning) {
                try {
                    // Wait for client connection
                    Socket clientSocket = serverSocket.accept();
                    int clientId = clientCounter.incrementAndGet();

                    System.out.println("New client connected! Client ID: " + clientId +
                                     " | Address: " + clientSocket.getInetAddress().getHostAddress());

                    // Requirement A (Creating Thread): Create a new thread for each client
                    ClientHandler clientHandler = new ClientHandler(clientSocket, inventoryManager);
                    Thread clientThread = new Thread(clientHandler, "Client-" + clientId);

                    // Start the client handler thread
                    clientThread.start();

                    System.out.println("Client-" + clientId + " thread started. Active threads: " +
                                     Thread.activeCount());

                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Server stopped.");
        }
    }

    /**
     * Stop the server
     */
    public void stop() {
        isRunning = false;
        System.out.println("Shutting down server...");
    }

    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        Server server = new Server();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received...");
            server.stop();
        }));

        // Start the server
        server.start();
    }

    /**
     * Get the inventory manager (used for testing)
     */
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
}

