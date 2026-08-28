# Flash Sale Concurrency Engine - System Design Document

## 1. Problem Statement
Handle extreme burst traffic where 100,000 concurrent virtual users attempt to purchase a limited inventory of 500 concert tickets at the exact same millisecond. 
**Strict Requirement:** ZERO overselling (Correctness and Data Integrity > Raw Throughput).

## 2. System Architecture (Oracle Cloud Infrastructure)
The system utilizes a distributed architecture to separate compute from state:
* **Edge Routing:** Cloudflare Proxy → OCI Flexible Load Balancer.
* **Compute Tier (App Nodes):** Java 21 / Spring Boot containers handling request routing and business logic.
* **State Tier (Data Nodes):** 
  * **Redis:** Acts as the high-speed atomic gatekeeper for inventory checks.
  * **MySQL:** Acts as the permanent ACID-compliant system of record.

## 3. Concurrency Strategy Evolution

### Phase 1: Pessimistic Locking (MySQL `FOR UPDATE`)
* **Mechanism:** Serialized database access using row-level locks.
* **Verdict:** Rejected for production. While it prevented overselling, it caused massive HikariCP connection pool starvation. Latency spiked to 59+ seconds under 50k VUs, eventually exhausting Linux TCP ports.

### Phase 2: Optimistic Locking (JPA `@Version`)
* **Mechanism:** Lock-free database version checking.
* **Verdict:** Rejected for high-contention flash sales. Resulted in massive `OptimisticLockException` collisions. Under a 25k VU spike, 99% of requests failed before the inventory could be fully depleted.

### Phase 3: Redis Lua Scripting (Production Architecture)
* **Mechanism:** Bypasses database locks entirely. A single-threaded Lua script executes atomically in Redis to check and decrement stock in $<2\text{ms}$.
* **Verdict:** Flawless. Processed 100,000 concurrent users with a peak throughput of 9,317 RPS. The database is shielded from traffic, only receiving `INSERT` commands for the 500 successful orders.

## 4. Database Schema (MySQL)

### Product Table
- `id` (PK, auto-increment)
- `name` (VARCHAR)
- `price` (DECIMAL)
- `total_stock` (INT) - Baseline inventory
- `available_stock` (INT) - Tracked for fallback/syncing
- `version` (BIGINT) - Legacy optimistic lock tracking

### Order Table
- `id` (PK, auto-increment)
- `product_id` (FK → Product)
- `user_id` (BIGINT)
- `quantity` (INT)
- `total_price` (DECIMAL)
- `status` (ENUM: SUCCESS, FAILED)
- `order_time` (TIMESTAMP)

## 5. Core API Endpoints

### The Hot Path: Purchase Ticket
`POST /api/orders/purchase-redis`
* **Flow:**
  1. Request hits Spring Boot.
  2. Spring Boot executes Lua script on Redis (`KEYS[1]: product_id`, `ARGV[1]: quantity`).
  3. If Lua returns `> 0` (Success): Open DB transaction, insert Order, return 200 OK.
  4. If Lua returns `-2` (Sold Out): Fast return 400 Bad Request, no DB connection opened.

### System Reset (Load Test Preparation)
`POST /api/test/reset-all`
* **Flow:** Truncates Order table, resets MySQL Product stock to 500, and synchronizes the Redis cache. Used strictly between k6 load test iterations.

## 6. Technology Stack
* **Backend:** Java 21, Spring Boot 3.x, Spring Data JPA
* **Databases:** MySQL 8.0, Redis
* **Infrastructure:** Docker, Oracle Cloud (Ampere A1 ARM), Nginx
* **Testing:** k6 (Distributed Load Testing)
