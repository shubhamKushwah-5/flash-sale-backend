# Flash Sale Concurrency Engine

> 🚀 **Live Production API**: https://flash-sale-backend-hd99.onrender.com
> 
> 🕹️ **Interactive API Testing**: Coming soon............
> 
High-performance ticket booking system preventing overselling under extreme concurrent load.

## 🎯 Problem Statement

How do you sell exactly 500 concert tickets when 10,000 people click "Buy" at the exact same millisecond?

This system solves the **race condition problem** that occurs in flash sales using database-level locking mechanisms.

## 🏗️ Architecture

- **Backend:** Java 21, Spring Boot 3.5.13, Spring Data JPA
- **Database:** PostgreSQL with row-level locking
- **Infrastructure:** Docker (multi-stage build), CI/CD via Render
- **Concurrency Strategy:** Pessimistic & Optimistic locking

## ⚡ Key Features

### 1. Pessimistic Locking (Strong Consistency Approach)
- **Mechanism:** `@Lock(LockModeType.PESSIMISTIC_WRITE)` on product row
- **Behavior:** Serializes access - first thread gets lock, others wait
- **Performance:** 460 req/sec, 959ms avg response time
- **Correctness:** ✅ PERFECT (500/500 tickets sold, zero overselling)

### 2. Optimistic Locking (For Normal Shopping)
- **Mechanism:** `@Version` field with retry logic (up to 3 attempts)
- **Behavior:** Concurrent reads allowed, version check on write
- **Performance:** 1820 req/sec, 60ms avg response time (4x faster)
- **Correctness:** ⚠️ 351/500 under extreme load (version conflicts exceeded retries)

## 📊 Load Test Results

**Test Configuration:**
- 1000 concurrent threads
- 2000 total purchase attempts
- 500 available tickets

**Pessimistic Locking:**
- ✅ Success: 500/500 (100% correctness)
- ⏱️ Duration: 4.3 seconds
- 📈 Throughput: 460 requests/second

**Optimistic Locking:**
- ⚠️ Success: 351/500 (version conflict failures)
- ⏱️ Duration: 1.1 seconds
- 📈 Throughput: 1820 requests/second

## 🔁 Optimistic Locking with Increased Retries (10 Attempts)

- Result: 314/500 tickets sold
- Observation: Minimal improvement despite higher retry limit

### Key Insight

Increasing retry attempts did not significantly improve success rate under extreme contention.
This is due to "retry storms", where failed transactions continuously conflict again during retries.
This demonstrates that optimistic locking alone is insufficient for high-contention scenarios like flash sales,
as it does not reduce contention—it only retries failed attempts.

**Conclusion:** Pessimistic locking guarantees correctness under extreme concurrent load by serializing access,
but introduces high latency and limited scalability.
Optimistic locking provides higher throughput but fails under extreme contention due to retry storms.

These experiments demonstrate that database-level locking alone is insufficient for large-scale flash sale systems.
Real-world systems mitigate contention using architectural patterns such as request queueing and in-memory stock management (e.g., Redis),
rather than relying solely on database locking strategies.

## 🔗 API Endpoints

### Products
```
POST   /api/products              Create product
GET    /api/products              Get all products
GET    /api/products/{id}         Get product by ID
GET    /api/products/{id}/stock   Check available stock
```

### Orders (Pessimistic Locking)
```
POST   /api/orders/purchase-pessimistic
Body: { "productId": 1, "userId": 101, "quantity": 2 }
```

### Orders (Optimistic Locking)
```
POST   /api/orders/purchase-optimistic
Body: { "productId": 1, "userId": 101, "quantity": 2 }
```

### Order History
```
GET    /api/orders/user/{userId}              User's orders
GET    /api/orders/product/{productId}/success-count
```

## 🧪 Testing

### Load Testing Infrastructure

Built custom load testing framework using:
- **ExecutorService** for thread pool management (1000 threads)
- **CountDownLatch** for synchronized thread start (simulates flash sale)
- **RestTemplate** for HTTP requests
- **Statistical analysis** (min, max, avg, P50, P95, P99)

See [LoadTestRunner.java](src/main/java/com/yourname/flashsaleengine/loadtest/LoadTestRunner.java)

### Running Load Tests Locally
```bash
mvn exec:java -Dexec.mainClass="com.shubham.flashsaleengine.loadtest.LoadTestRunner"
```

## 🐳 Docker Deployment

**Multi-stage build for optimized image size:**
```dockerfile
# Stage 1: Build with Maven + JDK (~800MB)
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with JRE only (387.86MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Local Docker run:**
```bash
docker build -t flash-sale-engine .
docker run -p 8080:8080 flash-sale-engine
```

## 📚 What I Learned

### Concurrency Mechanisms
- **Race conditions** and how they occur in concurrent systems
- **Pessimistic locking** serializes access for guaranteed correctness
- **Optimistic locking** allows concurrency but requires retry logic
- Database row-level locks (`SELECT FOR UPDATE`)

### Performance Trade-offs
- Correctness vs Speed: Pessimistic is 4x slower but 100% reliable
- Contention matters: Optimistic fails under extreme load
- Real-world decision: Flash sales need correctness > speed

### Testing Strategy
- Built load testing infrastructure from scratch
- Proved concurrency correctness with 1000 concurrent threads
- Measured performance with statistical analysis (percentiles)
- Verified database state (exactly 500 tickets sold)


## 🚀 Live Demo

**Try it yourself:**
```bash
# Create a product
curl -X POST https://flash-sale-backend-hd99.onrender.com/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Ticket","price":100,"stock":10}'

# Purchase with pessimistic locking
curl -X POST https://flash-sale-backend-hd99.onrender.com/api/orders/purchase-pessimistic \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"userId":1,"quantity":1}'
```

## 🛠️ Tech Stack

- Java 21
- Spring Boot 3.5.13
- Spring Data JPA
- PostgreSQL
- Docker
- Maven
- JUnit 5 (testing)

## 📖 Resources

- [Load Test Results](LOAD_TEST_RESULTS.md)
- [Design Document](DESIGN.md)
- [GitHub Repository] **Source Code:** [![GitHub Bage](https://img.shields.io/badge/GitHub-Repo-181717?style=flat&logo=github)](https://github.com/shubhamKushwah-5/flash-sale-backend.git)

---

**Built to demonstrate:**
- Concurrency control mechanisms
- Database locking strategies
- Load testing methodologies
- Performance analysis
- Production deployment (Docker + CI/CD)

## 📈 Future Scalability
While this project demonstrates DB-level locking, production-grade systems (like BookMyShow or Amazon) decouple inventory from the RDBMS:
- Distributed Locking: Using Redis + Lua Scripts to handle atomic decrements in-memory, achieving <5ms latency with 100% consistency.
- Virtual Queuing: Throttling ingress traffic to prevent database connection pool exhaustion.