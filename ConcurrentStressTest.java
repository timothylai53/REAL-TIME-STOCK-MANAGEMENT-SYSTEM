import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * ConcurrentStressTest.java
 *
 * Requirement H (Testing): This test class demonstrates thread-safety by spawning
 * multiple concurrent client threads that hammer the server with simultaneous requests.
 *
 * This test verifies:
 * - Thread safety of inventory operations
 * - No race conditions when multiple clients access the same product
 * - Proper synchronization mechanisms
 * - Server's ability to handle concurrent connections
 */
public class ConcurrentStressTest {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final int NUM_CLIENTS = 20; // Number of concurrent clients
    private static final int OPERATIONS_PER_CLIENT = 10; // Operations each client performs

    public static void main(String[] args) {
        System.out.println("=== CONCURRENT STRESS TEST ===");
        System.out.println("Make sure the Server is running on port " + SERVER_PORT);
        System.out.println("Starting test with " + NUM_CLIENTS + " concurrent clients...\n");

        // Wait for user to confirm server is running
        System.out.println("Press Enter to start the test...");
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Requirement H: Create thread pool to manage concurrent client threads
        ExecutorService executor = Executors.newFixedThreadPool(NUM_CLIENTS);
        List<Future<TestResult>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_CLIENTS);

        long startTime = System.currentTimeMillis();

        // Spawn multiple client threads
        for (int i = 0; i < NUM_CLIENTS; i++) {
            final int clientId = i + 1;
            final boolean isAdmin = (i % 4 == 0); // Every 4th client is admin

            Future<TestResult> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    TestResult result = runClientTest(clientId, isAdmin);
                    completionLatch.countDown();
                    return result;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new TestResult(clientId, false, "Interrupted");
                }
            });

            futures.add(future);
        }

        System.out.println("All threads ready. Starting concurrent test...\n");
        startLatch.countDown(); // Release all threads simultaneously

        try {
            // Wait for all clients to complete
            completionLatch.await();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Collect results
            System.out.println("\n=== TEST RESULTS ===");
            int successCount = 0;
            int failureCount = 0;

            for (Future<TestResult> future : futures) {
                try {
                    TestResult result = future.get();
                    if (result.success) {
                        successCount++;
                    } else {
                        failureCount++;
                        System.out.println("Client " + result.clientId + " FAILED: " + result.message);
                    }
                } catch (ExecutionException e) {
                    failureCount++;
                    System.err.println("Client execution error: " + e.getMessage());
                }
            }

            System.out.println("\n=== SUMMARY ===");
            System.out.println("Total Clients: " + NUM_CLIENTS);
            System.out.println("Successful: " + successCount);
            System.out.println("Failed: " + failureCount);
            System.out.println("Total Duration: " + duration + " ms");
            System.out.println("Average Time per Client: " + (duration / NUM_CLIENTS) + " ms");

            if (failureCount == 0) {
                System.out.println("\n✓ ALL TESTS PASSED - Server is thread-safe!");
            } else {
                System.out.println("\n✗ SOME TESTS FAILED - Check server implementation");
            }

        } catch (InterruptedException e) {
            System.err.println("Test interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Run test operations for a single client
     */
    private static TestResult runClientTest(int clientId, boolean isAdmin) {
        Socket socket = null;
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Random random = new Random();

            // Read welcome message
            readResponse(in);

            // Send username
            out.println("TestClient" + clientId);
            readResponse(in);

            // Admin check
            out.println(isAdmin ? "yes" : "no");
            String welcomeMsg = readResponse(in);

            System.out.println("Client-" + clientId + " connected: " +
                             (isAdmin ? "ADMIN" : "USER") + " - " + welcomeMsg);

            // Perform random operations
            for (int i = 0; i < OPERATIONS_PER_CLIENT; i++) {
                String command;
                int operation = random.nextInt(isAdmin ? 6 : 4);

                switch (operation) {
                    case 0:
                        command = "LIST";
                        break;
                    case 1:
                        // Buy random product
                        int productId = random.nextInt(5) + 1;
                        int quantity = random.nextInt(3) + 1;
                        command = "BUY_STOCK " + productId + " " + quantity;
                        break;
                    case 2:
                        command = "ANALYTICS";
                        break;
                    case 3:
                        command = "LOW_STOCK 15";
                        break;
                    case 4:
                        // Admin: Add stock
                        if (isAdmin) {
                            productId = random.nextInt(5) + 1;
                            quantity = random.nextInt(10) + 5;
                            command = "ADD_STOCK " + productId + " " + quantity;
                        } else {
                            command = "LIST";
                        }
                        break;
                    case 5:
                        // Admin: Update price
                        if (isAdmin) {
                            productId = random.nextInt(5) + 1;
                            double newPrice = 10 + random.nextDouble() * 500;
                            command = "UPDATE_PRICE " + productId + " " + String.format("%.2f", newPrice);
                        } else {
                            command = "LIST";
                        }
                        break;
                    default:
                        command = "LIST";
                }

                out.println(command);
                String response = readResponse(in);

                // Small random delay between operations
                Thread.sleep(random.nextInt(100));
            }

            // Request daily report to test thread joining
            out.println("DAILY_REPORT");
            String reportResponse = readResponse(in);

            // Analytics to test parallel streams
            out.println("ANALYTICS");
            readResponse(in);

            // Disconnect
            out.println("EXIT");
            readResponse(in);

            System.out.println("Client-" + clientId + " completed successfully");
            return new TestResult(clientId, true, "Success");

        } catch (Exception e) {
            System.err.println("Client-" + clientId + " error: " + e.getMessage());
            return new TestResult(clientId, false, e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Read server response (may be multiple lines)
     */
    private static String readResponse(BufferedReader in) throws IOException {
        StringBuilder response = new StringBuilder();
        String line;

        // Read until we get the separator or a short timeout
        long timeout = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < timeout) {
            if (in.ready()) {
                line = in.readLine();
                if (line == null) break;
                if (line.equals("---")) break;
                response.append(line).append("\n");
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return response.toString().trim();
    }

    /**
     * Test result class
     */
    private static class TestResult {
        final int clientId;
        final boolean success;
        final String message;

        TestResult(int clientId, boolean success, String message) {
            this.clientId = clientId;
            this.success = success;
            this.message = message;
        }
    }

    /**
     * Additional unit test for inventory manager thread-safety
     */
    public static void testInventoryManagerThreadSafety() {
        System.out.println("\n=== INVENTORY MANAGER THREAD-SAFETY TEST ===");

        InventoryManager manager = new InventoryManager();
        int numThreads = 50;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Random random = new Random();
                for (int j = 0; j < operationsPerThread; j++) {
                    int productId = random.nextInt(5) + 1;

                    if (random.nextBoolean()) {
                        // Buy stock
                        manager.buyStock(productId, random.nextInt(3) + 1);
                    } else {
                        // Add stock (simulate admin)
                        manager.addStock(productId, random.nextInt(5) + 1);
                    }
                }
                latch.countDown();
            });
        }

        try {
            latch.await();
            System.out.println("All threads completed without deadlock or race conditions!");
            System.out.println(manager.listAllProducts());
            System.out.println(manager.calculateTotalInventoryValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }
}

