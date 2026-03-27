# Flash Sale Concurrency Engine - Design Document

## Problem Statement
Handle 10,000 users trying to buy 500 concert tickets simultaneously.
Requirement: ZERO overselling (correctness > performance)

## Database Schema

### Product
- id (PK, auto-increment)
- name (String)
- price (BigDecimal)
- totalStock (Integer) - original inventory
- availableStock (Integer) - current inventory
- version (Long) - for optimistic locking
- createdAt (Timestamp)

### Order
- id (PK, auto-increment)
- productId (FK → Product)
- userId (Long) - simple user ID, no auth
- quantity (Integer)
- totalPrice (BigDecimal)
- status (ENUM: SUCCESS, FAILED)
- orderTime (Timestamp)

## API Endpoints

### 1. Create Product (Setup)
POST /api/products
Body: { name, price, totalStock }
Response: Product created

### 2. Purchase Ticket (Core - Pessimistic)
POST /api/orders/purchase-pessimistic
Body: { productId, userId, quantity }
Logic:
1. Start transaction
2. Lock Product row (PESSIMISTIC_WRITE)
3. Check availableStock >= quantity
4. If yes: decrement stock, create order, commit
5. If no: rollback, return error
   Response: Order created OR Error "Out of stock"

### 3. Purchase Ticket (Alternative - Optimistic)
POST /api/orders/purchase-optimistic
Body: { productId, userId, quantity }
Logic:
1. Read Product (no lock)
2. Check availableStock >= quantity
3. Try to decrement with version check
4. If version mismatch: retry up to 3 times
5. Create order if successful
   Response: Order created OR Error

### 4. Check Stock
GET /api/products/{id}/stock
Response: { availableStock, totalStock }

### 5. Order History
GET /api/orders/user/{userId}
Response: List of orders

## Concurrency Strategy

Primary: Pessimistic Locking
- Pros: Guaranteed correctness, simple logic
- Cons: Serializes requests, lower throughput
- Use case: High contention (flash sales)

Alternative: Optimistic Locking
- Pros: Higher throughput for low contention
- Cons: Retry logic needed, can fail under high load
- Use case: Normal shopping

## Load Testing Plan

Script: Java program with ExecutorService
- Thread pool: 1000 threads
- Each thread: POST /purchase-pessimistic
- Product: 500 tickets
- Requests: 2000 attempts (should sell exactly 500)

Metrics to measure:
- Success count (should be 500)
- Failure count (should be 1500)
- Average response time
- Max response time
- No overselling verified

## Technology Stack
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Docker
- JUnit 5 (testing)

## Future Enhancements (mention in README, don't build)
- Redis caching for stock reads
- Message queue for async processing (Kafka/RabbitMQ)
- Rate limiting per user
- Payment integration
- Multiple products in one order