# COPY-PASTE COMMANDS - Just copy and paste these into PowerShell!

## First Time Setup (Do Once)

# Navigate to project folder (if not already there)
cd "C:\Users\Timothy\Desktop\UUM\SEM 5\REAL TIME PROGRAMMING\GROUP PROJECT\REAL TIME STOCK MANAGEMENT SYSTEM"

# Compile all Java files (only needed once, or after code changes)
javac *.java

## Running the System

### TERMINAL 1 - Start Server
java Server

### TERMINAL 2 - Start Client 1
java SimpleClient

### TERMINAL 3 - Start Client 2
java SimpleClient

### TERMINAL 4 - Run Stress Test (server must be running)
java ConcurrentStressTest

## Using the Helper Script (Easier!)

### Compile
.\run.ps1 compile

### Start Server (Terminal 1)
.\run.ps1 server

### Start Client (Terminal 2, 3, 4...)
.\run.ps1 client

### Run Tests (New Terminal)
.\run.ps1 test

### Show Help
.\run.ps1 help

### Clean up .class files
.\run.ps1 clean

## Client Commands (After connecting)

### Regular User Commands
LIST
BUY_STOCK 1 2
BUY_STOCK 2 5
ANALYTICS
LOW_STOCK 20
DAILY_REPORT
HELP
EXIT

### Admin Commands (if you said "yes" to admin prompt)
ADD_PRODUCT Headphones 20 150.00
ADD_STOCK 1 10
UPDATE_PRICE 2 29.99
REMOVE_PRODUCT 5
LIST
EXIT

## Quick Test Sequence

### Test 1: Basic Functionality
# Terminal 1:
java Server

# Terminal 2:
java SimpleClient
# Then type:
Alice
no
LIST
BUY_STOCK 1 2
ANALYTICS
EXIT

### Test 2: Admin Features
# Terminal 1:
java Server

# Terminal 2:
java SimpleClient
# Then type:
AdminUser
yes
LIST
ADD_STOCK 1 20
UPDATE_PRICE 1 1099.99
LIST
EXIT

### Test 3: Multiple Concurrent Clients
# Terminal 1:
java Server

# Terminal 2:
java SimpleClient
# Type: User1, no, then: BUY_STOCK 1 2

# Terminal 3:
java SimpleClient
# Type: User2, no, then: BUY_STOCK 1 3

# Terminal 4:
java SimpleClient
# Type: User3, no, then: LIST

### Test 4: Automated Stress Test
# Terminal 1:
java Server

# Terminal 2:
java ConcurrentStressTest
# Press Enter when prompted

## Stopping the System

### To stop server:
Press Ctrl+C in the server terminal

### To disconnect client:
Type: EXIT

## Troubleshooting Commands

### Check if Java is installed
java -version

### Check if javac is installed
javac -version

### List files in current directory
dir

### Check if .class files exist
dir *.class

### Remove all .class files (if you need to recompile)
del *.class

### Kill process on port 8888 (if port is already in use)
netstat -ano | findstr :8888
# Note the PID, then:
taskkill /PID <PID_NUMBER> /F

## Demo Sequence for Presentation (5 minutes)

### Minute 1: Start System
# Terminal 1:
java Server

# Terminal 2:
java SimpleClient
AdminDemo
yes

# Terminal 3:
java SimpleClient
UserDemo
no

### Minute 2: Show Basic Features
# Terminal 2 (Admin):
LIST
ADD_STOCK 1 5

# Terminal 3 (User):
LIST
BUY_STOCK 1 2

### Minute 3: Show Advanced Features
# Terminal 2 (Admin):
UPDATE_PRICE 2 29.99
ANALYTICS

# Terminal 3 (User):
DAILY_REPORT

### Minute 4: Show Thread Safety
# Terminal 2:
BUY_STOCK 3 5

# Terminal 3 (at the same time):
BUY_STOCK 3 5

# Both:
LIST
# Notice: No race condition, inventory is correct!

### Minute 5: Stress Test
# Terminal 4:
java ConcurrentStressTest
# Press Enter
# Show: All 20 clients succeed, no deadlock!

## Verification Commands

### Verify Requirement A (Threads)
# Look at server console when clients connect
# You'll see: "Client-1 thread started", "Client-2 thread started", etc.

### Verify Requirement B (Priority)
# Connect as admin
# Welcome message shows: "[High Priority Thread]"

### Verify Requirement C (Joining)
# In client, type:
DAILY_REPORT
# You'll see: "Please wait" - thread is joining

### Verify Requirement D (No Deadlock)
java ConcurrentStressTest
# All 20 clients complete successfully = no deadlock

### Verify Requirement E (Synchronization)
# Multiple clients buy same product simultaneously
# Inventory stays consistent

### Verify Requirement F (ReentrantLock)
# Admin command:
UPDATE_PRICE 1 999.99
# This uses ReentrantLock (check InventoryManager.java line 95-115)

### Verify Requirement G (Parallel Streams)
ANALYTICS
# Uses parallelStream() (check InventoryManager.java line 120-145)

### Verify Requirement H (Testing)
java ConcurrentStressTest
# Comprehensive stress test with 20+ threads

---

# That's it! Your system is ready to run!
# Start with: .\run.ps1 server
# Then: .\run.ps1 client

