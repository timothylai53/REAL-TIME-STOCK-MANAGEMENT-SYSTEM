# Real-Time Stock Management System

## Overview
A multi-threaded, real-time stock management system built with Java (JDK 17+) using standard Java libraries only (no Spring Boot). This system demonstrates advanced concurrency concepts including thread management, synchronization, locks, parallel streams, and thread-safe operations.

## System Architecture

### Core Components

1. **Server.java** - Main server that listens for client connections on port 8888
2. **ClientHandler.java** - Handles individual client requests on separate threads
3. **InventoryManager.java** - Thread-safe inventory management with multiple synchronization techniques
4. **Product.java** - Data model for products
5. **ConcurrentStressTest.java** - Comprehensive testing with multiple concurrent clients
6. **SimpleClient.java** - Interactive client for manual testing

## Mandatory Requirements Implementation

### ✅ Requirement A: Creating Threads/Runnable
- **Location**: `Server.java` (lines ~45-55)
- **Implementation**: Server creates a new Thread for each client connection
- Each ClientHandler implements Runnable and runs on its own thread

### ✅ Requirement B: Thread Influencing
- **Location**: `ClientHandler.java` (lines ~45-52, ~220-245)
- **Implementation**: 
  - Admin users get higher thread priority (Priority 8) vs normal users (Priority 5)
  - Timeout mechanism using thread interruption for idle clients (300 seconds)

### ✅ Requirement C: Joining Threads
- **Location**: `ClientHandler.java` (lines ~155-175, ~180-215)
- **Implementation**: DAILY_REPORT command creates a ReportCalculator thread
- Main client thread calls `.join()` to wait for report completion

### ✅ Requirement D: Liveness/Deadlock Avoidance
- **Location**: `InventoryManager.java` (lines ~10-20)
- **Implementation**: Consistent lock ordering hierarchy
- **Strategy**: Always acquire inventoryLock first, then priceLock (if needed)
- Prevents circular wait conditions that cause deadlocks

### ✅ Requirement E: Synchronization
- **Location**: `InventoryManager.java` (multiple methods)
- **Implementation**: 
  - Synchronized methods: `addProduct()`, `addStock()`, `listAllProducts()`
  - Synchronized blocks in `buyStock()` method for atomic check-and-update

### ✅ Requirement F: Lock Interface
- **Location**: `InventoryManager.java` (lines ~95-115)
- **Implementation**: `updatePrice()` method uses ReentrantLock
- Demonstrates Lock interface as alternative to synchronized keyword

### ✅ Requirement G: Parallel Streams
- **Location**: `InventoryManager.java` (lines ~120-145)
- **Implementation**:
  - `calculateTotalInventoryValue()` - uses parallelStream() with reduce
  - `findLowStockItems()` - uses parallelStream() with filter and collect

### ✅ Requirement H: Testing
- **Location**: `ConcurrentStressTest.java`
- **Implementation**: Spawns 20+ concurrent client threads
- Each performs 10+ random operations to test thread-safety
- Includes separate inventory manager unit test

## Features

### User Commands
- **LIST** - View all products in inventory
- **BUY_STOCK <productId> <quantity>** - Purchase stock
- **ANALYTICS** - Calculate total inventory value (parallel processing)
- **LOW_STOCK [threshold]** - Find low-stock items (default: 20)
- **DAILY_REPORT** - Generate comprehensive report (demonstrates thread joining)
- **EXIT** - Disconnect from server

### Admin Commands (requires admin role)
- **ADD_PRODUCT <name> <quantity> <price>** - Add new product
- **ADD_STOCK <productId> <quantity>** - Add stock to existing product
- **UPDATE_PRICE <productId> <newPrice>** - Update product price (uses ReentrantLock)
- **REMOVE_PRODUCT <productId>** - Remove a product

## Compilation & Execution

### Windows PowerShell

#### Step 1: Compile all Java files
```powershell
javac *.java
```

#### Step 2: Start the Server
```powershell
java Server
```

#### Step 3: Run Interactive Client (in a new terminal)
```powershell
java SimpleClient
```

#### Step 4: Run Stress Test (in a new terminal, with server running)
```powershell
java ConcurrentStressTest
```

### Quick Start Script
Use the provided `run.ps1` script:
```powershell
# Compile
.\run.ps1 compile

# Start server
.\run.ps1 server

# Start client (new terminal)
.\run.ps1 client

# Run tests (new terminal, server must be running)
.\run.ps1 test
```

## Testing Instructions

### Manual Testing
1. Start the Server: `java Server`
2. Open multiple terminals and run: `java SimpleClient`
3. Test as admin user (username: admin, admin prompt: yes)
4. Test as regular user (username: user1, admin prompt: no)
5. Try concurrent operations from multiple clients

### Automated Stress Testing
1. Ensure server is running
2. Run: `java ConcurrentStressTest`
3. Press Enter when prompted
4. Observe 20 concurrent clients performing operations
5. Check the summary for success/failure rates

### Sample Test Scenarios

**Test Thread Safety:**
- Run 2+ clients simultaneously
- Have both clients buy the same product at the same time
- Verify inventory doesn't go negative (protected by synchronization)

**Test Admin Priority:**
- Connect one admin and one regular user
- Observe admin thread gets higher priority in logs

**Test Thread Joining:**
- Run DAILY_REPORT command
- Observe the waiting message while report thread executes

**Test Parallel Streams:**
- Run ANALYTICS command
- System uses parallel streams for calculation

**Test Deadlock Prevention:**
- Run stress test with 20+ concurrent clients
- System should handle all requests without deadlock

## Sample Inventory
The system initializes with:
1. Laptop (ID: 1) - Qty: 10, Price: $999.99
2. Mouse (ID: 2) - Qty: 50, Price: $25.50
3. Keyboard (ID: 3) - Qty: 30, Price: $75.00
4. Monitor (ID: 4) - Qty: 15, Price: $299.99
5. USB Cable (ID: 5) - Qty: 100, Price: $9.99

## Thread Safety Guarantees

1. **Inventory Operations**: Protected by synchronized methods and ReentrantLock
2. **Product Updates**: Atomic operations using synchronized blocks
3. **Concurrent Access**: Multiple clients can safely access inventory simultaneously
4. **No Race Conditions**: Proper synchronization prevents data corruption
5. **Deadlock Free**: Consistent lock ordering prevents deadlocks

## Technical Specifications

- **Java Version**: JDK 17+
- **Networking**: java.net.ServerSocket
- **Concurrency**: java.util.concurrent.locks, synchronized keyword
- **Parallel Processing**: Stream API with parallelStream()
- **Port**: 8888 (configurable in Server.java)

## Architecture Highlights

### Thread Model
- One thread per client connection
- Separate thread for report generation (joined)
- Thread pool for stress testing
- Daemon threads for timeout monitoring

### Synchronization Strategy
- ReentrantLock for price updates (fine-grained control)
- Synchronized methods for inventory operations
- Synchronized blocks for product-level operations
- Lock ordering: inventoryLock → priceLock (prevents deadlock)

### Concurrency Patterns
- Producer-Consumer: Clients produce requests, handlers consume
- Thread-per-connection: Scalable client handling
- Lock splitting: Separate locks for different operations
- Parallel processing: Analytics using parallel streams

## Grading Rubric Coverage

| Requirement | Implementation | File | Lines |
|------------|----------------|------|-------|
| A - Threads/Runnable | New thread per client | Server.java | 45-55 |
| B - Thread Influencing | Priority + Interrupt | ClientHandler.java | 45-52, 220-245 |
| C - Joining Threads | Daily report with join | ClientHandler.java | 155-215 |
| D - Liveness/Deadlock | Lock ordering | InventoryManager.java | 10-20 |
| E - Synchronization | synchronized methods | InventoryManager.java | Multiple |
| F - Lock Interface | ReentrantLock | InventoryManager.java | 95-115 |
| G - Parallel Streams | Analytics features | InventoryManager.java | 120-145 |
| H - Testing | Stress test | ConcurrentStressTest.java | Full file |

## Authors
Created for UUM Real-Time Programming Course (Semester 5)
December 2025

## License
Educational use only

