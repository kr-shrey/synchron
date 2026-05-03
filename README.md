# synchron
 
Bi-directional record-to-record synchronization

## Architecture documentation

[View Architecture](docs/architecture.md)

## What has been implemented

I have implemented the sync pipeline for a single record going from Internal System → External System. The implementation includes:

1. InternalSystemSchema: for an internal record
2. ExampleExternalSystemSchema: for an external record/payload
3. BBidirectional transformer with versioning and a registry pattern
4. Rule engine that decides whether/how to sync based on configurable rules; returns a `Decision` (Sync or Drop)
5. Token-bucket rate limiter with per-key fine-grained locking (in-memory; production would be Redis-backed)
6. Idempotency check using `ConcurrentHashMap.compute()` to filter duplicates
7. Sync worker that ties it all together with retries, DLQ, and error handling

The Demo application creates and executes 200 events. These events have randomly generated data with some heuristics to mimic a real-world scenario.

## How to build and run the demo

### Prerequisites

Java: requires JDK 21. Ensure `JAVA_HOME` is configured.
Maven: ensure Maven is installed and accessible via the terminal

Setup: Clone the repository and navigate to the directory containing the `pom.xml` file.

### Build the fat JAR

```bash
mvn package
```

An executable JAR file will be created in the `target` directory. You can navigate there using `cd target`.

### Run the JAR application

```bash
java -jar synchron-1.0-SNAPSHOT.jar
```