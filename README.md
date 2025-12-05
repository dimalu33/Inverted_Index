# Inverted Index Server (Parallel Search Engine)
This project implements a high-performance **parallel full-text search server** based on an **Inverted Index**. It is written in **Java (Maven)** and uses **multithreading** for background file indexing and concurrent processing of client search requests. The indexer automatically monitors the data directory `the_link` and asynchronously processes new or updated text files using a custom thread pool (`CustomThreadPool`).

## 1. Prerequisites
To build and run the project, you need:
- **Java Development Kit (JDK) 11+**
- **Apache Maven**
- **Data Directory:** create a folder named `the_link` in the `server` module root — this folder contains the text files that will be indexed.

## 2. Setup and Build Instructions
Clone the repository:
```bash
git clone https://github.com/dimalu33/Inverted_Index.git
cd Inverted_Index
```

Create the indexing directory:
```bash
mkdir server/the_link
```
Place your .txt files inside server/the_link

Build the project:
```bash
cd server
mvn clean package
```

After the build, an executable JAR will appear in `server/target/`.

## 3. Running the Server
Start the server:
```bash
cd target
java -jar Inverted_Index-1.0-SNAPSHOT.jar
```

The server will start on **port 9999**, index all files in `the_link`, and listen for client connections.

## 4. Client Usage
The server uses a simple **line-based text protocol**.

### A. Java Search Client (`Main.java`)

Run the Java client from project root:
```bash
mvn exec:java -Dexec.mainClass="org.example.Main"
```

Usage:
Search > your_term
Type 'quit' to exit.

### B. C++ Client (`main.cpp`)
Compile on Linux/macOS:
```bash
g++ CppClient/main.cpp -o client
```

Compile on Windows (MinGW):
```bash
g++ CppClient/main.cpp -o client.exe -lws2_32
```

Run:
```bash
./client
```

Usage:
Enter your search query.
Type 'quit' or 'exit' to disconnect.

### C. Load Tester (`LoadTester.java`)
Run the load tester:
```bash
mvn exec:java -Dexec.mainClass="org.example.LoadTester"
```
The tester measures:
- Requests per second (RPS)
- Average latency
- Performance under different concurrency levels (10 → 4000 users)
and outputs detailed performance metrics.
