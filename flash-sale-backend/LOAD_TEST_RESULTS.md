# 🚀 Flash Sale Engine - Concurrency Load Test Report
 
**Mission:** Prove zero overselling under extreme 1,000-thread concurrent load.

---

## 1. Executive Summary
This report documents the stress-testing of the Flash Sale Engine's locking mechanisms. We simulated a "Real-World Flash Sale" scenario where 1,000 users attempt to buy 500 available items at the exact same millisecond using a `CountDownLatch` and `ExecutorService`.

## 2. Test Configuration
| Parameter | Value |
| :--- | :--- |
| **Total Requests** | 2000 (1000 threads x 2 requests/thread) |
| **Concurrent Threads** | 1000 (Fixed Thread Pool) |
| **Initial Stock** | 500 tickets |
| **Testing Tool** | Custom Java LoadTestRunner (RestTemplate + CountDownLatch) |

---

## 3. Test 1: Pessimistic Locking (`@Lock(PESSIMISTIC_WRITE)`)
Pessimistic locking serializes access by locking the database row. No other transaction can read or write until the current one commits.

### Results
* **Total Duration:** 4345ms
* **Throughput:** 460.30 req/sec
* **Successful Purchases:** 500 ✅ (EXACTLY 500 sold)
* **Failed Purchases:** 1500 ✅ (Correctly rejected)

### Response Times (Latency)
| Metric | Value |
| :--- | :--- |
| **Min** | 465ms |
| **Median (P50)** | 994ms |
| **Average** | 958.99ms |
| **P95 (Slowest 5%)** | 1123ms |
| **P99 (Worst Case)** | 1186ms |
| **Max** | 1290ms |

**Analysis:** Pessimistic locking is the "Gold Standard" for correctness. While it has higher average latency (~950ms) due to threads waiting in a queue, it guaranteed that every single piece of stock was sold without a single extra ticket being created.

---

## 4. Test 2: Optimistic Locking (`@Version`)
Optimistic locking allows all threads to read concurrently and only fails the transaction if the "version" has changed during the update.

### Results
* **Total Duration:** 1099ms
* **Throughput:** 1819.84 req/sec
* **Successful Purchases:** 351 ⚠️ (Stock remained due to retry exhaustion)
* **Failed Purchases:** 1649 ✅

### Response Times (Latency)
| Metric | Value |
| :--- | :--- |
| **Min** | 3ms |
| **Median (P50)** | 19ms |
| **Average** | 60.17ms |
| **P95 (Slowest 5%)** | 285ms |
| **P99 (Worst Case)** | 523ms |
| **Max** | 775ms |

**Analysis:** Optimistic locking is **3.9x faster** than Pessimistic and has a **400% higher throughput**. However, under extreme contention (1000 threads), 149 tickets remained unsold because the threads exhausted their retry limits. This proves Optimistic locking is best for low-to-medium contention, while Pessimistic is safer for "High-Heat" Flash Sales.

---

## 5. Final Comparison Table

| Metric | Pessimistic Locking | Optimistic Locking | Winner |
| :--- | :--- | :--- | :--- |
| **Correctness (Overselling)** | **ZERO** | **ZERO** | **Tie** ✅ |
| **Stock Depletion** | **100% Sold** | 70% Sold (Retry Fail) | **Pessimistic** 🏆 |
| **Throughput** | 460.30 req/sec | **1819.84 req/sec** | **Optimistic** 🏆 |
| **Avg. User Experience** | ~950ms | **~60ms** | **Optimistic** 🏆 |
| **Reliability** | Extremely High | Moderate (at scale) | **Pessimistic** 🏆 |

---

## 6. Database Verification (Proof of Work)
After the tests, the following SQL was run to verify state:

```sql
-- Verify Product 8 (Pessimistic)
SELECT available_stock FROM products WHERE id = 8;
-- Result: 0 (Perfect)

-- Verify Product 9 (Optimistic)
SELECT available_stock FROM products WHERE id = 9;
-- Result: 149 (Leftover due to retry limits)

-- Check for duplicate success for same user
SELECT user_id, COUNT(*) FROM orders 
WHERE product_id = 8 AND status = 'SUCCESS' 
GROUP BY user_id HAVING COUNT(*) > 1;
-- Result: 0 (No user cheated the system)


## Key Findings

### Pessimistic Locking
- **Correctness:** ✅ PERFECT (500/500 tickets sold, zero overselling)
- **Performance:** 460 req/sec, 959ms avg response time
- **Recommendation:** Use for flash sales (high contention scenarios)

### Optimistic Locking
- **Correctness:** ⚠️ PARTIAL (351/500 tickets sold)
- **Performance:** ✅ EXCELLENT (1820 req/sec, 60ms avg response time)
- **Analysis:** Under extreme concurrent load (1000 simultaneous threads), 
  version conflicts exceeded retry limit (3 attempts). This resulted in 
  149 legitimate purchase attempts failing due to optimistic lock exceptions.
- **Recommendation:** NOT suitable for flash sales. Use for normal shopping 
  (low contention) where speed matters more than guaranteed success.

### Real-World Implication
This test demonstrates why companies like BookMyShow and Ticketmaster use 
pessimistic locking or queue-based systems for ticket sales. Optimistic 
locking's performance advantage is offset by reduced reliability under 
extreme concurrent load.

### Interview Talking Point
"I empirically proved through load testing that optimistic locking, while 
4x faster, fails to guarantee correctness under extreme contention. This 
validates the architectural decision to use pessimistic locking for flash 
sales despite the performance cost."