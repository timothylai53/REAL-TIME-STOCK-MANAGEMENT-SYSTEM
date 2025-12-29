import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * ClientHandler.java
 * Requirement A (Threads/Runnable): This class implements Runnable to handle each client
 * on a separate thread. Each connected client gets its own ClientHandler instance running
 * on its own thread.
 *
 * Requirement B (Thread Influencing): Admin users get higher thread priority (PRIORITY 8)
 * compared to normal users (PRIORITY 5). Additionally, we implement a timeout mechanism
 * using thread interruption for idle clients.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final InventoryManager inventoryManager;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean isAdmin;
    private static final int TIMEOUT_SECONDS = 300; // 5 minutes timeout

    public ClientHandler(Socket socket, InventoryManager inventoryManager) {
        this.clientSocket = socket;
        this.inventoryManager = inventoryManager;
        this.isAdmin = false;
    }

    @Override
    public void run() {
        try {
            // Initialize I/O streams
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Welcome message
            out.println("=== REAL-TIME STOCK MANAGEMENT SYSTEM ===");
            out.println("Enter your username:");
            username = in.readLine();

            if (username == null || username.trim().isEmpty()) {
                out.println("ERROR: Invalid username. Disconnecting.");
                return;
            }

            // Check if admin user
            out.println("Are you an admin? (yes/no):");
            String adminResponse = in.readLine();
            isAdmin = adminResponse != null && adminResponse.equalsIgnoreCase("yes");

            // Requirement B (Thread Influencing): Set thread priority based on user role
            if (isAdmin) {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 2); // Priority 8
                out.println("Welcome Admin " + username + "! [High Priority Thread]");
            } else {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY); // Priority 5
                out.println("Welcome " + username + "!");
            }

            out.println("\nType HELP for available commands.");

            // Requirement B: Start timeout monitor thread
            TimeoutMonitor timeoutMonitor = new TimeoutMonitor();
            Thread timeoutThread = new Thread(timeoutMonitor);
            timeoutThread.setDaemon(true);
            timeoutThread.start();

            // Main command loop
            String command;
            while ((command = in.readLine()) != null) {
                timeoutMonitor.resetTimeout(); // Reset timeout on activity

                String response = processCommand(command.trim());
                out.println(response);
                out.println("---");
            }

        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                sendMessage("Connection timed out due to inactivity. Disconnecting...");
            } else {
                System.err.println("Error handling client " + username + ": " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Process client commands and return response
     */
    private String processCommand(String command) {
        if (command.isEmpty()) {
            return "ERROR: Empty command";
        }

        String[] parts = command.split("\\s+");
        String action = parts[0].toUpperCase();

        try {
            switch (action) {
                case "HELP":
                    return getHelpMessage();

                case "LIST":
                    return inventoryManager.listAllProducts();

                case "ADD_PRODUCT":
                    // ADD_PRODUCT <name> <quantity> <price>
                    if (!isAdmin) {
                        return "ERROR: Only admins can add new products";
                    }
                    if (parts.length < 4) {
                        return "ERROR: Usage: ADD_PRODUCT <name> <quantity> <price>";
                    }
                    String name = parts[1];
                    int qty = Integer.parseInt(parts[2]);
                    double price = Double.parseDouble(parts[3]);
                    return inventoryManager.addProduct(name, qty, price);

                case "ADD_STOCK":
                    // ADD_STOCK <productId> <quantity>
                    if (!isAdmin) {
                        return "ERROR: Only admins can add stock";
                    }
                    if (parts.length < 3) {
                        return "ERROR: Usage: ADD_STOCK <productId> <quantity>";
                    }
                    int productId = Integer.parseInt(parts[1]);
                    int addQty = Integer.parseInt(parts[2]);
                    return inventoryManager.addStock(productId, addQty);

                case "BUY_STOCK":
                    // BUY_STOCK <productId> <quantity>
                    if (parts.length < 3) {
                        return "ERROR: Usage: BUY_STOCK <productId> <quantity>";
                    }
                    int buyProductId = Integer.parseInt(parts[1]);
                    int buyQty = Integer.parseInt(parts[2]);
                    return inventoryManager.buyStock(buyProductId, buyQty);

                case "UPDATE_PRICE":
                    // UPDATE_PRICE <productId> <newPrice>
                    if (!isAdmin) {
                        return "ERROR: Only admins can update prices";
                    }
                    if (parts.length < 3) {
                        return "ERROR: Usage: UPDATE_PRICE <productId> <newPrice>";
                    }
                    int priceProductId = Integer.parseInt(parts[1]);
                    double newPrice = Double.parseDouble(parts[2]);
                    return inventoryManager.updatePrice(priceProductId, newPrice);

                case "ANALYTICS":
                    // Requirement G: Parallel stream analytics
                    return inventoryManager.calculateTotalInventoryValue();

                case "LOW_STOCK":
                    // LOW_STOCK <threshold>
                    int threshold = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
                    return inventoryManager.findLowStockItems(threshold);

                case "DAILY_REPORT":
                    // Requirement C: Joining threads for report generation
                    return generateDailyReport();

                case "REMOVE_PRODUCT":
                    if (!isAdmin) {
                        return "ERROR: Only admins can remove products";
                    }
                    if (parts.length < 2) {
                        return "ERROR: Usage: REMOVE_PRODUCT <productId>";
                    }
                    int removeId = Integer.parseInt(parts[1]);
                    return inventoryManager.removeProduct(removeId);

                case "EXIT":
                    sendMessage("Goodbye " + username + "!");
                    Thread.currentThread().interrupt();
                    return "Disconnecting...";

                default:
                    return "ERROR: Unknown command. Type HELP for available commands.";
            }
        } catch (NumberFormatException e) {
            return "ERROR: Invalid number format";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Requirement C (Joining Threads): Generate daily report using a separate thread
     * The main client handler thread creates a calculation thread, waits for it to
     * complete using .join(), and then returns the result.
     */
    private String generateDailyReport() {
        try {
            out.println("Generating daily report... Please wait.");

            // Create a report calculation thread
            ReportCalculator reportCalculator = new ReportCalculator();
            Thread reportThread = new Thread(reportCalculator);

            // Requirement C: Start the thread and join (wait for completion)
            reportThread.start();
            reportThread.join(); // Main thread waits here until report thread completes

            return reportCalculator.getReport();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: Report generation interrupted";
        }
    }

    /**
     * Requirement C: Inner class for report calculation running on separate thread
     */
    private class ReportCalculator implements Runnable {
        private String report;

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== DAILY INVENTORY REPORT ===\n");
            sb.append("Generated by: ").append(username).append("\n");
            sb.append("Timestamp: ").append(new java.util.Date()).append("\n\n");

            // Simulate complex calculation
            try {
                Thread.sleep(1000); // Simulate processing time
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            List<Product> products = inventoryManager.getAllProducts();

            int totalProducts = products.size();
            int totalQuantity = products.stream().mapToInt(Product::getQuantity).sum();
            double totalValue = products.stream().mapToDouble(Product::getTotalValue).sum();

            sb.append("Total Product Types: ").append(totalProducts).append("\n");
            sb.append("Total Units in Stock: ").append(totalQuantity).append("\n");
            sb.append(String.format("Total Inventory Value: $%.2f\n", totalValue));
            sb.append("\nProduct Breakdown:\n");

            for (Product p : products) {
                sb.append(String.format("  - %s: %d units @ $%.2f = $%.2f\n",
                        p.getName(), p.getQuantity(), p.getPrice(), p.getTotalValue()));
            }

            sb.append("\n=== END OF REPORT ===");
            this.report = sb.toString();
        }

        public String getReport() {
            return report;
        }
    }

    /**
     * Requirement B: Timeout monitor to interrupt idle connections
     */
    private class TimeoutMonitor implements Runnable {
        private volatile long lastActivityTime;

        public TimeoutMonitor() {
            resetTimeout();
        }

        public void resetTimeout() {
            lastActivityTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000); // Check every 10 seconds

                    long idleTime = (System.currentTimeMillis() - lastActivityTime) / 1000;
                    if (idleTime > TIMEOUT_SECONDS) {
                        // Requirement B: Interrupt the client handler thread
                        System.out.println("Client " + username + " timed out after " + idleTime + " seconds");
                        Thread.currentThread().interrupt();
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * Get help message with available commands
     */
    private String getHelpMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== AVAILABLE COMMANDS ===\n");
        sb.append("LIST - List all products\n");
        sb.append("BUY_STOCK <productId> <quantity> - Purchase stock\n");
        sb.append("ANALYTICS - Show total inventory value (uses parallel processing)\n");
        sb.append("LOW_STOCK [threshold] - Find low stock items (default threshold: 20)\n");
        sb.append("DAILY_REPORT - Generate comprehensive daily report\n");

        if (isAdmin) {
            sb.append("\n=== ADMIN COMMANDS ===\n");
            sb.append("ADD_PRODUCT <name> <quantity> <price> - Add new product\n");
            sb.append("ADD_STOCK <productId> <quantity> - Add stock to existing product\n");
            sb.append("UPDATE_PRICE <productId> <newPrice> - Update product price\n");
            sb.append("REMOVE_PRODUCT <productId> - Remove a product\n");
        }

        sb.append("\nEXIT - Disconnect from server\n");
        return sb.toString();
    }

    /**
     * Send message to client
     */
    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            System.out.println("Client disconnected: " + username);
        } catch (IOException e) {
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }
}

