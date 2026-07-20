# Device Memory Demo — Interface Projections vs Class-Based DTO Projections

A Spring Boot 2.7 / Java 8 application that reproduces a real-world Spring Data JPA
memory issue: **interface projections create one JDK dynamic proxy per returned row**,
while **class-based DTO (constructor) projections instantiate plain Java objects
directly**. The app loads ~30,000 rows from PostgreSQL and exposes two endpoints that
return **identical JSON**, differing only in the projection mechanism — so heap dumps
taken under load can be compared side by side in VisualVM, Eclipse MAT, or JMC.

## Tech Stack

- Java 8
- Spring Boot 2.7.x (Spring Data JPA, Hibernate)
- PostgreSQL (Docker Compose)
- Maven
- Lombok

## Project Layout

```
src/main/java/com/example/devicememory/
├── DeviceMemoryApplication.java
├── config/DataInitializer.java        # seeds ~30,000 rows via JDBC batch inserts
├── controller/DeviceController.java   # /api/interface, /api/dto
├── controller/StressController.java   # /api/stress/interface/{count}, /api/stress/dto/{count}
├── dto/DeviceDTO.java                 # immutable POJO, constructor projection
├── entity/Device.java
├── projection/DeviceProjection.java   # interface projection (dynamic proxies)
├── repository/DeviceRepository.java
└── service/DeviceService.java
```

## 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL on `localhost:5432` with database `device_db`,
user `postgres`, password `postgres`.

## 2. Run the Application

Build:

```bash
mvn clean package
```

Run with a constrained heap and automatic heap dumps on OOM:

```bash
mkdir -p heapdumps

java -Xms512m -Xmx512m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=./heapdumps \
     -jar target/device-memory-demo-1.0.0.jar
```

Or during development:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdumps"
```

The 512 MB heap is deliberate: it makes heap growth visible quickly and lets the
stress endpoints push the JVM toward (or into) an OutOfMemoryError, which then
produces a heap dump automatically.

## 3. Data Generation

On startup, if the `device` table is empty, the app inserts ~30,000 rows using JDBC
batch inserts (batches of 1,000). Look for this in the logs:

```
Device table is empty, generating 30000 rows
Inserted 30000 device rows
```

On subsequent startups the seeding is skipped.

## 4. Call the APIs

Both endpoints return the **same JSON** — an array of ~30,000 objects:

```bash
curl http://localhost:8080/api/interface
curl http://localhost:8080/api/dto
```

Sample element:

```json
{
  "make": "Samsung",
  "model": "Galaxy S24",
  "esimCompatibility": "YES",
  "fivegCompatibility": "SUPPORTED"
}
```

- `/api/interface` — Spring Data **interface projection** (`DeviceProjection`).
  Spring Data returns a JDK dynamic proxy per row, each backed by a
  `TupleBackedMap` holding the column values.
- `/api/dto` — **class-based DTO projection** via a JPQL constructor expression
  (`SELECT new com.example.devicememory.dto.DeviceDTO(...)`). Hibernate invokes the
  `DeviceDTO` constructor directly for each row.

## 5. Create Memory Pressure

The stress endpoints call the underlying query `{count}` times and **retain every
returned list in memory until the request completes**:

```bash
# 30 iterations × 30,000 rows = 900,000 proxy objects held simultaneously
curl http://localhost:8080/api/stress/interface/30

# Same, but 900,000 plain DTO objects
curl http://localhost:8080/api/stress/dto/30
```

If a stress call completes too quickly to capture a dump, raise the count
(e.g. `/api/stress/interface/100`) — with a 512 MB heap this will typically trigger
`OutOfMemoryError`, and `-XX:+HeapDumpOnOutOfMemoryError` will write a dump to
`./heapdumps` automatically.

## 6. Capture a Heap Dump

While a stress request is in flight (or right before it finishes):

```bash
# find the PID
jps -l | grep device-memory-demo

# capture a dump of live objects
jmap -dump:live,format=b,file=heapdumps/interface-projection.hprof <PID>
```

Repeat for the DTO endpoint:

```bash
curl http://localhost:8080/api/stress/dto/30 &
jmap -dump:live,format=b,file=heapdumps/dto-projection.hprof <PID>
```

Alternatives:
- **VisualVM**: attach to the process → *Monitor* tab (watch heap grow live) →
  *Heap Dump* button.
- **JMC**: Flight Recorder or `jcmd <PID> GC.heap_dump heapdumps/dump.hprof`.

## 7. Analyze in Eclipse MAT

1. *File → Open Heap Dump* → select the `.hprof` file.
2. Run the **Leak Suspects** report, or open the **Histogram**.
3. In the Histogram, sort by **Retained Heap**.

### What to look for in the interface-projection dump

- Classes named `com.sun.proxy.$Proxy…` / `jdk.proxy…` — one instance **per row
  returned**. With 30 iterations of 30,000 rows, expect ~900,000 proxy instances.
- `org.springframework.data.projection.*` support objects
  (e.g. `TupleBackedMap`, accessor/invocation-handler infrastructure) referenced
  by each proxy.
- Right-click a proxy class → *Merge Shortest Paths to GC Roots* to see the retained
  list chain back to the in-flight HTTP request.

### What to look for in the DTO dump

- `com.example.devicememory.dto.DeviceDTO` instances — plain objects with exactly
  four `String` reference fields each and no supporting structures.

### Compare

- Histogram → filter by `Proxy` in one dump and `DeviceDTO` in the other.
- Compare **shallow size × instance count** and total **retained heap** for the
  row-object classes. The interface-projection dump carries the proxies *plus* their
  backing maps and handler objects; the DTO dump carries only the DTOs.
- MAT can also diff dumps directly: open both, then *Histogram → Compare to another
  Heap Dump*.

## Why DTOs Use Less Memory

For every row it returns, a Spring Data **interface projection** builds:

1. a **JDK dynamic proxy instance** implementing `DeviceProjection`,
2. an **invocation handler** that routes `getMake()` etc. at runtime,
3. a **backing map** (`TupleBackedMap`) holding the column name → value entries.

That is several objects (plus map entry overhead) per row, and every getter call goes
through reflection-style dispatch. Multiply by 30,000 rows × N retained lists and
the intermediary objects dominate the heap.

A **constructor-based DTO projection** produces exactly **one plain object per row**:
Hibernate reads the tuple and calls `new DeviceDTO(make, model, esim, fiveg)`. There
is no proxy, no handler, no backing map — just four references in a POJO. Same JSON
out, fewer objects and less retained heap.

## Logging

Each request logs its lifecycle and result size:

```
Starting interface projection request
Completed interface projection request, number of records returned: 30000
Starting DTO projection request
Completed DTO projection request, number of records returned: 30000
```
