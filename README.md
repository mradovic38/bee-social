# 🐝 BeeSocial - Distributed Image Sharing System

BeeSocial is a Java-based distributed system that enables decentralized sharing and management of 
.jpg image files across networked nodes. Designed to simulate a minimal social platform, BeeSocial includes 
key distributed systems principles such as self-organization, fault tolerance, and eventual consistency, 
while supporting user interactions via a simple command-line interface.

## 📌 Features

* **Distributed File System**
  * Add and distribute .jpg image files across nodes
  * Browse and fetch files from other nodes
  * Remove files from the local node

* **Node Interactions**
  * Follow/unfollow nodes
  * Visibility control (public/private profiles)
  
* **Topology-aware Communication**
  * Peer-to-peer **Chord** architecture
  * Logarithmic message routing for scalability
    
* **Fault Tolerance**
  * Dual-phase failure detection: weak and strong failure boundaries
  * Buddy system for failover and data recovery

* **Bootstrap-less Self-Organization**
  * Bootstrap server only introduces a node into the system, with no further control
  * Decentralized, resilient structure with dynamic join/leave capability
    
* **Fair Distributed Mutual Exclusion**
  * Fair mutex implementation using **Suzuki–Kasami** algorithm


## 🧪 Commands (CLI Interface)

| Command                       | Description                                  |
| ----------------------------- | -------------------------------------------- |
| `follow [ip:port]`            | Sends a follow request to the specified node |
| `pending`                     | Lists incoming follow requests               |
| `accept [ip:port]`            | Accepts a follow request                     |
| `upload [relative_path]`      | Uploads a file to the network                |
| `visibility [public/private]` | Sets the visibility of your files            |
| `list_files [ip:port]`        | Lists files of another node (if allowed)     |
| `remove_file [filename]`      | Removes file from the local node             |
| `quit`                        | Gracefully exits the node from the system    |
| `stop`                        | Violently shuts down the node from the system|


## ⚙️ Configuration

See file [`chord/servent_list.properties`](chord/servent_list.properties)

* `servent_count`: Number of servents in the system
* `chord_size`: Capacity of the Chord architecture
* `root`: Local working directory for storing files
* `cache`: Local working directory for saving files gathered using commands
* `servent{x}.port`: Listening port for the node `{x}`
* `bs,port`: Listening port for the bootstrap node
* `weak_limit`: Time before suspecting a node (ms)
* `strong_limit`: Time before removing a node (ms)

**Note:** Config files must be consistent across all nodes.


## 📡 System Architecture

BeeSocial supports the Chord architecture: 
* ✅ Fully decentralized
* ✅ Logarithmic routing
* ✅ Fault-resilient, dynamic restructuring


## 🛠️ Fault Detection

* **Phase 1: Weak Suspect**
  * Node unresponsive beyond the weak threshold
  * Cross-verification by another stable node (buddy node)

* **Phase 2: Strong Failure**
  * Node unresponsive beyond strong threshold
  * Node removed from the system
  * Buddy nodes take over responsibilities

## 🗃️ Data Redundancy

Each file is stored on the originating node, and at least one replica exists in the system to support fault recovery.

## 🔒 Distributed Mutual Exclusion

Supports a **fair mutex algorithm** to control access to shared resources:
* Implemented using Suzuki–Kasami token-based algorithm

## 🧳 Project Structure

```
bee-social/
├── src/
│   ├── app/                       # System management
│   ├── cli/                       # Command-line interface
│   ├── fault_tolerance/           # Fault tolerance logic
│   ├──mutex/                      # Mutex implementation
│   └── servent/                   # Node logic
├── chord/
│   ├── error/                     # Error debug log for each node
│   ├── input/                     # Input commands for each node
│   ├── output/                    # Output debug log for each node
│   └── servent_list.properties    # System configuration
├── cache/                         # Directory for saving files gathered using commands
├── root/                          # Directory for storing files
├── README.md
└── ...
```


## 🧪 Running the System

1. Compile the project:

   ```bash
   javac -d out src/**/*.java
   ```

2. Start [`MultipleServentStarter`](src/app/MultipleServentStarter.java):

   ```bash
   java -cp out app.MultipleServentStarter
   ```

---


## 📚 Technologies Used

* Java 11+
* Sockets for communication
* Custom-built CLI parser
* Threading and timers for failure detection

## 📖 Resources
* [Suzuki–Kasami Algorithm for Mutual Exclusion in Distributed System - GeeksForGeeks](https://www.geeksforgeeks.org/operating-systems/suzuki-kasami-algorithm-for-mutual-exclusion-in-distributed-system/)
* [Deep dive into Chord for Distributed Hash Table - Abhijit Mondal](https://mecha-mind.medium.com/deep-dive-into-chord-for-distributed-hash-table-e54f1b3411b)
