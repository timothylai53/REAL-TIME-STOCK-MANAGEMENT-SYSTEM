import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * InventoryManager.java
 * Thread-safe inventory management system using multiple synchronization techniques.
 *
 * Requirement D (Liveness/Deadlock): This class avoids deadlock by using a consistent
 * locking order. We use only ONE lock (inventoryLock) for most operations and a separate
 * lock (priceLock) for price updates only. When both locks are needed, we ALWAYS acquire
 * inventoryLock first, then priceLock, ensuring a consistent lock ordering hierarchy.
 * This prevents circular wait conditions that cause deadlocks.
 */
public class InventoryManager {
    // Requirement E (Synchronization): Using synchronized methods/blocks
    private final List<Product> products;

    // Requirement F (Lock Interface): Using ReentrantLock for price updates
    private final ReentrantLock priceLock;
    private final ReentrantLock inventoryLock;

    private int nextProductId;

    public InventoryManager() {
        this.products = new ArrayList<>();
        this.priceLock = new ReentrantLock(true); // Fair lock
        this.inventoryLock = new ReentrantLock(true);
        this.nextProductId = 1;

        // Initialize with some sample products
        initializeSampleProducts();
    }

    private void initializeSampleProducts() {
        addProduct("Laptop", 10, 999.99);
        addProduct("Mouse", 50, 25.50);
        addProduct("Keyboard", 30, 75.00);
        addProduct("Monitor", 15, 299.99);
        addProduct("USB Cable", 100, 9.99);
    }

    /**
     * Requirement E (Synchronization): Synchronized method to add a new product
     */
    public synchronized String addProduct(String name, int quantity, double price) {
        inventoryLock.lock();
        try {
            Product product = new Product(nextProductId++, name, quantity, price);
            products.add(product);
            return "SUCCESS: Product added - " + product.toString();
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Requirement E (Synchronization): Synchronized method to add stock
     */
    public synchronized String addStock(int productId, int quantity) {
        inventoryLock.lock();
        try {
            Optional<Product> productOpt = findProductById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setQuantity(product.getQuantity() + quantity);
                return "SUCCESS: Added " + quantity + " units to " + product.getName() +
                       ". New quantity: " + product.getQuantity();
            } else {
                return "ERROR: Product ID " + productId + " not found";
            }
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Requirement E (Synchronization): Synchronized block for purchasing stock
     */
    public String buyStock(int productId, int quantity) {
        inventoryLock.lock();
        try {
            Optional<Product> productOpt = findProductById(productId);
            if (productOpt.isEmpty()) {
                return "ERROR: Product ID " + productId + " not found";
            }

            Product product = productOpt.get();

            // Synchronized block to ensure atomic check-and-update
            synchronized (product) {
                if (product.getQuantity() < quantity) {
                    return "ERROR: Insufficient stock. Available: " + product.getQuantity() +
                           ", Requested: " + quantity;
                }

                product.setQuantity(product.getQuantity() - quantity);
                double totalCost = quantity * product.getPrice();
                return String.format("SUCCESS: Purchased %d units of %s. Total: $%.2f. Remaining stock: %d",
                                   quantity, product.getName(), totalCost, product.getQuantity());
            }
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Requirement F (Lock Interface): Using ReentrantLock for updating product prices
     * This demonstrates the Lock interface as an alternative to synchronized keyword
     */
    public String updatePrice(int productId, double newPrice) {
        // Requirement D: Consistent lock ordering - inventoryLock first, then priceLock
        inventoryLock.lock();
        try {
            Optional<Product> productOpt = findProductById(productId);
            if (productOpt.isEmpty()) {
                return "ERROR: Product ID " + productId + " not found";
            }

            Product product = productOpt.get();

            // Requirement F: Using ReentrantLock instead of synchronized
            priceLock.lock();
            try {
                double oldPrice = product.getPrice();
                product.setPrice(newPrice);
                return String.format("SUCCESS: Price updated for %s. Old: $%.2f, New: $%.2f",
                                   product.getName(), oldPrice, newPrice);
            } finally {
                priceLock.unlock();
            }
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Requirement G (Parallel Streams): Analytics feature using parallel streams
     * Calculates total inventory value using parallel processing
     */
    public String calculateTotalInventoryValue() {
        inventoryLock.lock();
        try {
            // Using parallelStream() for parallel processing
            double totalValue = products.parallelStream()
                    .mapToDouble(Product::getTotalValue)
                    .reduce(0.0, Double::sum);

            return String.format("Total Inventory Value: $%.2f", totalValue);
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Requirement G (Parallel Streams): Find low-stock items using parallel stream
     */
    public String findLowStockItems(int threshold) {
        inventoryLock.lock();
        try {
            List<Product> lowStockProducts = products.parallelStream()
                    .filter(p -> p.getQuantity() < threshold)
                    .collect(Collectors.toList());

            if (lowStockProducts.isEmpty()) {
                return "No low-stock items found (threshold: " + threshold + ")";
            }

            StringBuilder result = new StringBuilder("Low Stock Items (< " + threshold + "):\n");
            lowStockProducts.forEach(p -> result.append(p.toString()).append("\n"));
            return result.toString();
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Get all products as a formatted string
     */
    public synchronized String listAllProducts() {
        inventoryLock.lock();
        try {
            if (products.isEmpty()) {
                return "No products in inventory";
            }

            StringBuilder sb = new StringBuilder("=== INVENTORY LIST ===\n");
            for (Product p : products) {
                sb.append(p.toString()).append("\n");
            }
            sb.append("Total Products: ").append(products.size());
            return sb.toString();
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Helper method to find product by ID
     */
    private Optional<Product> findProductById(int id) {
        return products.stream()
                .filter(p -> p.getId() == id)
                .findFirst();
    }

    /**
     * Get a copy of all products for reporting purposes
     */
    public synchronized List<Product> getAllProducts() {
        inventoryLock.lock();
        try {
            // Return a defensive copy
            return new ArrayList<>(products);
        } finally {
            inventoryLock.unlock();
        }
    }

    /**
     * Remove a product (admin operation)
     */
    public synchronized String removeProduct(int productId) {
        inventoryLock.lock();
        try {
            Optional<Product> productOpt = findProductById(productId);
            if (productOpt.isPresent()) {
                products.remove(productOpt.get());
                return "SUCCESS: Product removed - " + productOpt.get().getName();
            } else {
                return "ERROR: Product ID " + productId + " not found";
            }
        } finally {
            inventoryLock.unlock();
        }
    }
}

