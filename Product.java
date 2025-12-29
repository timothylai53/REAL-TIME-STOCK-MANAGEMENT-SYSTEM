/**
 * Product.java
 * Data model representing a product in the inventory system.
 * This class is immutable for thread-safety when sharing product information.
 */
public class Product {
    private final int id;
    private String name;
    private int quantity;
    private double price;

    public Product(int id, String name, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    // Setters (used internally by InventoryManager)
    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return String.format("Product[ID=%d, Name=%s, Qty=%d, Price=%.2f]",
                           id, name, quantity, price);
    }

    /**
     * Calculate the total value of this product (quantity * price)
     */
    public double getTotalValue() {
        return quantity * price;
    }
}

