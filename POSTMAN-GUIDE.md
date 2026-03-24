# Postman Collection Usage Guide

## Setup

1. **Download Postman**: [Download Postman](https://www.postman.com/downloads/)

2. **Import the Collection**:
   - Open Postman
   - Click the "Import" button in the top-left corner
   - Select the `Monat-Ecommerce-API.postman_collection.json` file
   - Click the "Import" button

## Environment Variables

The collection automatically includes the following variables:

### Service URLs
- `USER_SERVICE_URL`: http://localhost:8081
- `PRODUCT_SERVICE_URL`: http://localhost:8082
- `CART_SERVICE_URL`: http://localhost:8084
- `ORDER_SERVICE_URL`: http://localhost:8085

### Dynamic Variables (To be populated during testing)
- `USER_ID`: User UUID
- `PRODUCT_ID`: Product ID
- `CART_ID`: Cart ID
- `ANONYMOUS_CART_ID`: Anonymous cart ID
- `ORDER_ID`: Order UUID
- `ORDER_NUMBER`: Order number

## Test Flow

### 1. User Registration
```
POST /api/users/register
```
- Registers a new user
- Copy the `userId` from the response and paste it into the `USER_ID` variable

### 2. Product Creation
```
POST /api/products
```
- Creates a new product
- Copy the `productId` from the response and paste it into the `PRODUCT_ID` variable

### 3. Adding Product to Cart
```
POST /api/cart/{cartId}/items
```
- Enter a value for the `CART_ID` variable (e.g., "user-123" or "session-abc")
- Adds a product to the cart

### 4. Viewing Cart
```
GET /api/cart/{cartId}
```
- Displays cart contents

### 5. Order Creation

#### Option A: Direct Order
```
POST /api/orders (Direct)
```
- Creates an order by specifying products directly

#### Option B: Order from Cart
```
POST /api/orders (From Cart)
```
- Creates an order from the existing cart
- The cart is automatically deleted after the order is created

## Endpoint Groups

### User Service (Port 8081)
- ✅ Register User
- ✅ Get User by ID
- ✅ Get User by Email
- ✅ Get All Users (Paginated)
- ✅ Add Address
- ✅ Get User Addresses
- ✅ Validate User

### Product Service (Port 8082)
- ✅ Create Product
- ✅ Update Product
- ✅ Get Product by ID
- ✅ Get All Products (Paginated)
- ✅ Get Products by Category
- ✅ Search Products (Elasticsearch)
- ✅ Delete Product

### Cart Service (Port 8084)
- ✅ Get Cart
- ✅ Add Item to Cart
- ✅ Update Item Quantity
- ✅ Remove Item from Cart
- ✅ Clear Cart
- ✅ Merge Carts (Anonymous + User)

### Order Service (Port 8085)
- ✅ Create Order (Direct)
- ✅ Create Order (From Cart) **[NEW]**
- ✅ Get Order by ID
- ✅ Get Order by Number
- ✅ Get User Orders (Paginated)

## Example Test Scenario

### Full E2E Flow:

1. **User Registration**
   ```
   POST /api/users/register
   → Save USER_ID
   ```

2. **Product Creation**
   ```
   POST /api/products
   → Save PRODUCT_ID
   ```

3. **Add to Cart**
   ```
   POST /api/cart/user-{{USER_ID}}/items
   → CART_ID = "user-{{USER_ID}}"
   ```

4. **Verify Cart**
   ```
   GET /api/cart/user-{{USER_ID}}
   ```

5. **Order from Cart**
   ```
   POST /api/orders (From Cart)
   → Save ORDER_ID
   ```

6. **View Order**
   ```
   GET /api/orders/{{ORDER_ID}}
   ```

## Notes

- All services must be running with Docker Compose
- You can also test via Swagger UI:
  - User: http://localhost:8081/swagger-ui.html
  - Product: http://localhost:8082/swagger-ui.html
  - Cart: http://localhost:8084/swagger-ui.html
  - Order: http://localhost:8085/swagger-ui.html

## Troubleshooting

### Services not running?
```bash
docker-compose ps
docker-compose up -d
```

### Database connection error?
```bash
docker-compose restart
```

### Port conflict?
Change the ports in the `docker-compose.yml` file.
