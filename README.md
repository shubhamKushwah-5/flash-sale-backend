# ⚡ Flash Sale Concurrency Engine

<div align="center">
  <img src="https://img.shields.io/badge/Java%2021-20232a?style=flat-square&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-20232a?style=flat-square&logo=spring&logoColor=6DB33F" />
  <img src="https://img.shields.io/badge/Redis-20232a?style=flat-square&logo=redis&logoColor=DC382D" />
  <img src="https://img.shields.io/badge/MySQL-20232a?style=flat-square&logo=mysql&logoColor=4479A1" />
  <img src="https://img.shields.io/badge/Docker-20232a?style=flat-square&logo=docker&logoColor=2496ED" />
  <img src="https://img.shields.io/badge/Oracle%20Cloud-20232a?style=flat-square&logo=oracle&logoColor=F80000" />
</div>

<br/>

A high-throughput, distributed ticket booking REST API engineered to prevent overselling during extreme e-commerce checkout surges. 

🚀 **Live System Portfolio:** [shubhamkushwahportfolio.site](https://shubhamkushwahportfolio.site)

---

## 🏗️ Cloud Infrastructure & Topology
The system is deployed on a distributed 4-node **Oracle Cloud Infrastructure (OCI)** cluster using Ampere A1 ARM compute shapes, completely containerized via Docker. 

*   **App Tier (2 Nodes):** 8 OCPUs, 32GB RAM each. Running the Spring Boot application behind an Nginx reverse proxy.
*   **State Tier (1 Node):** 16 OCPUs, 64GB RAM. Hosting MySQL and Redis containers. 
*   **Test Runner (1 Node):** 8 OCPUs, 32GB RAM. Dedicated entirely to running distributed k6 load tests to prevent local network bottlenecks.

  ```text
┌─────────────────────────────────────────────────────────┐
│                 k6 TEST RUNNER NODE                     │
│       (100,000 Virtual Users firing concurrently)       │
│               [Oracle Cloud VCN]                        │
└───────────────────────────┬─────────────────────────────┘
                            │ (Internal Network Traffic)
                            ▼
                    ┌───────────────┐
                    │  NGINX PROXY  │ (App Nodes 1 & 2)
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  CONTROLLER   │
                    │/purchase-redis│
                    └───────┬───────┘
                            │
                            ▼
                  ┌───────────────────┐
                  │    REDIS CACHE    │ (State Node 3)
                  │   (Lua Script)    │ <── ATOMIC INVENTORY GATEKEEPER
                  └─────────┬─────────┘
                            │ 
          [If Stock > 0]    │    [If Stock == 0]
          ┌─────────────────┴──────────────────┐
          ▼                                    ▼
  ┌───────────────┐                    ┌───────────────┐
  │  DB SERVICE   │                    │ FAST REJECTION│
  │@Transactional │                    │ (0ms DB Time) │
  └───────┬───────┘                    └───────────────┘
          │
          ▼
  ┌───────────────┐
  │     MySQL     │ (State Node 3)
  │ (Insert Order)│
  └───────────────┘

```


*   **Networking:** Instances communicate over a private Virtual Cloud Network (VCN). Public traffic is routed through Cloudflare to an OCI Flexible Load Balancer (120 Mbps limit), with strictly configured `iptables` allowing only port 80/443 ingress.

---

## 🎯 The Problem: Race Conditions at Scale
During a flash sale, thousands of concurrent users attempt to purchase a limited stock (e.g., 500 tickets) simultaneously. If multiple threads read the same database stock value before any thread commits a decrement, the system will process more orders than available inventory, resulting in **overselling**.

## ⚙️ Architectural Evolution & Benchmarks
This system evolved through three concurrency control strategies. Each was stress-tested using k6 over a real cloud network.

### 1. Pessimistic Locking (Row-Level DB Lock)
- **Mechanism:** Used `PESSIMISTIC_WRITE` via Spring Data JPA (`SELECT ... FOR UPDATE`).
- **Result:** Prevented overselling, but forced threads to wait in line, causing catastrophic HikariCP connection pool starvation.
- **Bottleneck:** At 10,000 VUs, throughput dropped to 276 RPS. At 50,000+ VUs, average latency degraded to 79 seconds, dropping tens of thousands of requests and crashing the server.

### 2. Optimistic Locking (Entity Versioning)
- **Mechanism:** Used `@Version` annotations with a retry mechanism to catch `OptimisticLockException`.
- **Result:** Failed to sell out the inventory. Under a sudden 25,000 VU spike, only 205 out of 500 items were sold. 1 thread succeeded while 24,999 threads threw version conflict exceptions before the remaining inventory could be processed.

### 3. The Solution: Redis Lua Gatekeeper (Current Architecture)
- **Mechanism:** Decoupled the hot-path inventory check from the database. Implemented an **atomic Lua script** executing in-memory on the Redis cluster. 
- **The DB Shield Pattern:** Redis acts as an absolute gatekeeper. Out of 100,000 incoming requests, 99,500 are instantly rejected by Redis in memory. Only the 500 "winners" proceed to execute an `INSERT` against MySQL. This completely eliminates database lock contention and connection pool exhaustion.

---

## 📊 Performance & Load Testing Results

📊 Comprehensive Load Test Results
Extreme Spike Tests (The 1-Click Flash Sale Rush)
Simulates a real-world flash sale where all virtual users fire a single purchase request simultaneously at exactly $T = 0$.

| Strategy | Spike VUs | Items Sold | Avg Latency | p(95) Latency | Failure & Behavior Analysis |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Optimistic** | 25,000 | **205 / 500** | 10.19 s | 21.08 s | **CRITICAL FAILURE.** Entity version collisions rejected 99% of threads; failed to sell out inventory. |
| **Pessimistic** | 25,000 | 500 / 500 | 37.13 s | 56.01 s | Unusable UX; users waited nearly a minute for order responses. |
| **Redis Lua** | 25,000 | 500 / 500 | 1.40 s | 2.33 s | Sub-3s p(95) response time under 25k concurrent spike. |
| **Pessimistic** | 50,000 | 500 / 500 | 19.63 s | 59.08 s | Connection pool choked; triggered OS TCP port exhaustion. |
| **Redis Lua** | 50,000 | 500 / 500 | 1.60 s | 3.31 s | Handled 50k concurrent requests smoothly; zero dropouts. |
| **Pessimistic** | 100,000 | 500 / 500 | 13.38 s | 55.39 s | Total system lockup; HikariCP exhausted; massive packet loss. |
| **Redis Lua** | **100,000** | **500 / 500** | **1.53 s** | **2.95 s** | **Ultimate Target.** 4,299 req/s throughput; 0 oversold. |

| Metric | Redis Lua Architecture | MySQL Pessimistic Locking |
| :--- | :--- | :--- |
| **Peak Throughput** | **9,317 RPS** | 460 RPS |
| **Optimal Latency p(95)** | **754 ms** (at 5,000 VUs) | 17.66 s (at 10,000 VUs) |
| **Extreme Scale Test** | **100,000 Concurrent VUs** | System Crash at 50,000 VUs |
| **Oversold Count** | **0** | 0 |

### 🛑 Hitting the Linux OS Limit
During the 100,000 VU ramp-up test, the application code did not fail. The bottlenecks encountered were purely physical infrastructure limits:
1.  **Ephemeral Port Exhaustion:** The k6 load runner node threw `cannot assign requested address` because the Linux OS ran out of available outbound TCP ports. 
2.  **TCP Backlog Overflow:** The application threw `i/o timeout` logs because the Linux TCP backlog queue filled up, dropping packets before Tomcat could accept the connections. 

---

## 💻 Core Logic: The Lua Script
Because Redis is single-threaded, executing a Lua script guarantees absolute atomicity without distributed locking overhead.

```lua
-- If product cache doesn't exist, return -1 (Cache Miss)
if redis.call('exists', KEYS[1]) == 0 then return -1 end 

local stock = tonumber(redis.call('get', KEYS[1])) 
local qty = tonumber(ARGV[1]) 

-- If stock is available, decrement and return remaining. Else, return -2 (Sold Out).
if stock >= qty then 
    return redis.call('decrby', KEYS[1], qty) 
else 
    return -2 
end
```

---

## 🚀 Quick Start (Local Deployment)

Run the backend locally using Docker Compose.

### Prerequisites
- Docker & Docker Compose
- JDK 21 (for manual Maven builds)

### Installation
```bash
git clone [https://github.com/shubhamKushwah-5/flash-sale-backend.git](https://github.com/shubhamKushwah-5/flash-sale-backend.git)
cd flash-sale-backend

# Build the application container and spin up Redis/MySQL
docker-compose up -d --build

# Monitor startup logs
docker-compose logs -f app
```
The API will be exposed on `http://localhost:8080`.

---

## 📝 API Endpoints

### 1. Execute Purchase (The Hot Path)
**POST** `/api/orders/purchase-redis`
```json
{
  "userId": 4059,
  "productId": 101,
  "quantity": 1
}
```
**Response (Success):**
```json
{
  "success": true,
  "message": "Order placed successfully",
  "orderId": 5928
}
```

### 2. Reset System for Load Testing
**POST** `/api/test/reset-all`
*Wipes all database orders, resets MySQL product stock/versions, and synchronizes the Redis cache to prepare for a fresh load test.*

---

## 👨‍💻 Author
**Shubham Kushwah**
* **Portfolio:** [shubhamkushwahportfolio.site](https://shubhamkushwahportfolio.site)
* **LinkedIn:** [Shubham Kushwah](https://www.linkedin.com/in/shubham-kushwah-5657912b8/)
* **LeetCode:** [Shubhamkushwah0777 (Rating: 1650)](https://leetcode.com/u/Shubhamkushwah0777/)
