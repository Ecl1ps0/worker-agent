# Worker Agent

This project implements a **Worker Agent** using the **JADE (Java Agent DEvelopment Framework)**. It is designed to operate as a node in a distributed system, accepting computational tasks (specifically model training), executing them, and reporting results back to a main server.

## Architecture

The Worker Agent operates as follows:
1.  **Registration**: On startup, it registers itself with the JADE Directory Facilitator (DF) as a `model-trainer` service.
2.  **System Monitoring**: It uses **OSHI** to monitor system load and responds to `QUERY_IF` messages with current load metrics, allowing the main agent to load-balance tasks.
3.  **Task Execution**:
    -   Receives `REQUEST` messages containing a URL to an executable file (the training task).
    -   Downloads the executable.
    -   Runs the executable locally in a separate process.
    -   Captures the output and waits for a `trained_model.pkl` file to be generated.
4.  **Result Upload**: Uploads the generated model to a central server and confirms completion to the requestor.

## Tools and Libraries Used

-   **Language**: Java 17
-   **Build Tool**: Maven
-   **Agent Framework**: [JADE 4.5.0](https://jade.tilab.com/) - Communication and agent management.
-   **System Monitoring**: [OSHI (Operating System and Hardware Information)](https://github.com/oshi/oshi) 6.6.1 - Used to retrieve system load averages.
-   **Native Access**: JNA (Java Native Access) - Required by OSHI.
-   **Packaging**:
    -   `maven-shade-plugin`: Creates an uber-jar with all dependencies.
    -   `launch4j-maven-plugin`: Wraps the JAR into a Windows executable (`.exe`).


## Dependencies

The project relies on a local `jade.jar` which must be installed manually into the local Maven repository (see instructions below).

**Python Environment**:
The worker expects a Python environment to be available if the downloaded tasks are Python executables or require Python libraries. You should ensure the following are installed:
-   `scikit-learn`, `xgboost`, `numpy`, `scipy`
-   `pyzmq`, `psutil`, `pyinstaller`
-   `jupyter`, `ipython`

## Installation and Build

### Prerequisites
-   Java JDK 17
-   Maven

### 1. Install JADE
One external dependency (`jade.jar`) is provided in the `lib` folder and must be installed locally:

```bash
mvn install:install-file -Dfile="./lib/jade.jar" -DgroupId="jade" -DartifactId="jade" -Dversion="4.5.0" -Dpackaging="jar"
```

### 2. Build the Project
To clean and build the project, generating both the JAR and the EXE:

```bash
mvn clean install
```

This will produce artifacts in the `target/` directory:
-   `worker_agent-1.0-SNAPSHOT-shaded.jar` (Cross-platform JAR)
-   `worker_agent.exe` (Windows Executable)

## Running the Agent

You can run the agent in several ways. The agent requires a `MAIN_HOST` property to know where the JADE Main Container is running.

### Option 1: Using Maven (Development)
```bash
mvn exec:java -DMAIN_HOST="192.168.10.4"
```
*Replace `192.168.10.4` with the IP of your Main Container.*

### Option 2: Running the JAR (Cross-Platform)
Works on Windows, Linux, and macOS.
```bash
java -jar target/worker_agent-1.0-SNAPSHOT-shaded.jar -main-host 192.168.10.4
```
*Note: You may need to pass the host as a system property instead:*
```bash
java -DMAIN_HOST="192.168.10.4" -jar target/worker_agent-1.0-SNAPSHOT-shaded.jar
```

### Option 3: Running the EXE

#### Windows
Simply double-click `worker_agent.exe` or run from command line:
```cmd
target\worker_agent.exe
```

#### Linux and macOS (Process Emulation)
> **Note:** The `.exe` file is a Windows executable. To run it on Linux or macOS, you must use **Wine**.

1.  Install Wine (e.g., `sudo apt install wine` on Ubuntu or `brew install --cask wine-stable` on macOS).
2.  Run the executable:
    ```bash
    wine target/worker_agent.exe
    ```