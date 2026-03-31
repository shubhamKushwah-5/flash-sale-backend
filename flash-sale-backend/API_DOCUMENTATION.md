# Flash Sale Engine - API Documentation

## Base URL
**Local:** `http://localhost:8080`  
**Production:** ` https://flash-sale-backend-hd99.onrender.com`

---

## Product Endpoints

### Create Product
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
```json
{
  "id": 1,
  "name": "Concert Ticket - Coldplay",
  "price": 5000,
  "totalStock": 500,
  "availableStock": 500,
  "version": 0,
  "createdAt": "2025-03-28T10:30:00"
}
```

---

### Get All Products
**GET** `/api/products`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Concert Ticket - Coldplay",
    "price": 5000,
    "totalStock": 500,
    "availableStock": 450,
    "version": 50,
    "createdAt": "2025-03-28T10:30:00"
  }
]
```

---

### Get Product by ID
**GET** `/api/products/{id}`

**Response:** `200 OK` or `404 Not Found`

---

### Check Stock
**GET** `/api/products/{id}/stock`

**Response:** `200 OK`
```json
{
  "availableStock": 450,
  "totalStock": 500
}
```

---

## Order Endpoints

### Purchase with Pessimistic Locking
**POST** `/api/orders/purchase-pessimistic`

**Use Case:** Flash sales, high-contention scenarios

**Request Body:**
```json
{
  "productId": 1,
  "userId": 1001,
  "quantity": 2
}
```

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Purchase successful",
  "orderId": 1,
  "status": "SUCCESS"
}
```

**Failure Response:** `400 Bad Request`
```json
{
  "success": false,
  "message": "Insufficient stock",
  "orderId": null,
  "status": "FAILED"
}
```

---

### Purchase with Optimistic Locking
**POST** `/api/orders/purchase-optimistic`

**Use Case:** Normal shopping, low-contention scenarios

**Request/Response:** Same as pessimistic

**Note:** May retry up to 3 times on version conflicts

---

### Get User Orders
**GET** `/api/orders/user/{userId}`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "productId": 1,
    "userId": 1001,
    "quantity": 2,
    "totalPrice": 10000,
    "status": "SUCCESS",
    "orderTime": "2025-03-28T11:00:00"
  }
]
```

---

### Get Successful Order Count
**GET** `/api/orders/product/{productId}/success-count`

**Response:** `200 OK`
```json
450
```

---

## Error Responses

### Product Not Found
```json
{
  "timestamp": "2025-03-28T12:00:00",
  "message": "Product not found with id: 999",
  "status": 400
}
```

### Validation Error
```json
{
  "timestamp": "2025-03-28T12:00:00",
  "message": "Quantity must be at least 1",
  "status": 400
}
```

---

## Testing with cURL
```bash
# Create product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Ticket","price":100,"stock":10}'

# Purchase (pessimistic)
curl -X POST http://localhost:8080/api/orders/purchase-pessimistic \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"userId":1,"quantity":2}'

# Check stock
curl http://localhost:8080/api/products/1/stock
```
## Postman Collection
```
> You can also test all APIs using the included Postman collection.
  📁 [Download Collection](./postman/FlashSaleAPI.postman_collection.json)
  
  ### How to use:
1. Open Postman
2. Click "Import"
3. Select the downloaded JSON file
4. Start testing APIs
```
---

## Load Testing

See [LoadTestRunner.java](src/main/java/com/shubham/flashsale/loadtest/LoadTestRunner.java)
```bash
# Run load test
mvn exec:java -Dexec.mainClass="com.shubham.flashsale.loadtest.LoadTestRunner"
```

**Test Configuration:**
- 1000 concurrent threads
- 2000 total requests
- Tests both pessimistic and optimistic locking