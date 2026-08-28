# Flash Sale Engine - API Documentation

## Base URLs
**Local Development:** `http://localhost:8080`

---

## 📦 Product Endpoints

### 1. Create Product (Setup)
**POST** `/api/products`

**Request Body:**
```json
{
  "name": "Concert Ticket - Coldplay",
  "price": 5000,
  "stock": 500
}
```
**Response:** `201 Created`

### 2. Get All Products
**GET** `/api/products`

**Response:** `200 OK` (Returns an array of all products and their current stock).

### 3. Check Specific Product Stock
**GET** `/api/products/{id}/stock`

**Response:** `200 OK`
```json
{
  "availableStock": 450,
  "totalStock": 500
}
```

---

## 🛒 Order Endpoints

### 1. Purchase Ticket (Redis Optimized - HOT PATH)
**POST** `/api/orders/purchase-redis`

*Use Case: Extreme Flash Sales. Uses Lua scripting to prevent DB locks.*

**Request Body:**
```json
{
  "productId": 1,
  "userId": 1001,
  "quantity": 1
}
```
**Response (Success):** `200 OK`
```json
{
  "success": true,
  "message": "Order placed successfully",
  "orderId": 1
}
```
**Response (Sold Out):** `400 Bad Request`
```json
{
  "success": false,
  "message": "Insufficient stock"
}
```

### 2. Purchase Ticket (Standard Pessimistic DB Lock)
**POST** `/api/orders/purchase-pessimistic`

*Use Case: Legacy endpoint for load-test benchmarking.*

**Request Body:** Same as above.

### 3. Get User Order History
**GET** `/api/orders/user/{userId}`

**Response:** `200 OK` (Returns an array of a specific user's successful and failed orders).

---

## 🛠️ Testing & Utilities

### System Reset (For Load Testing)
**POST** `/api/test/reset-all`

*Action: Wipes all database orders, resets MySQL product stock to 500, and synchronizes the Redis cache.*

### Postman Collection
You can test all APIs using the included Postman collection in the repository.
📁 `[Download Collection](./postman/FlashSaleAPI.postman_collection.json)`

### Load Testing
Distributed load testing was performed using **k6** on Oracle Cloud. Please refer to the main `README.md` for performance benchmarks and architecture details.
