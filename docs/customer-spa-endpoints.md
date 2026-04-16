# Customer SPA Endpoints

This document lists the endpoints expected to be used by a customer-facing SPA.

Base URL through API Gateway:

```text
http://localhost:8080
```

Gateway routes are defined in `api-gateway/src/main/resources/application.yml`.

## Customer-Facing REST Endpoints

| Area | Method | Endpoint | Purpose | Auth |
|---|---:|---|---|---|
| Auth | `POST` | `/api/users/register` | Register a new customer | No |
| Auth | `POST` | `/api/users/login` | Login and receive JWT | No |
| Profile | `GET` | `/api/users/{userId}` | Get user profile | Not enforced at gateway |
| Address | `POST` | `/api/users/{userId}/addresses` | Add a user address | Not enforced at gateway |
| Address | `GET` | `/api/users/{userId}/addresses` | List user addresses | Not enforced at gateway |
| Cart | `GET` | `/api/cart/{cartId}` | Get user or anonymous cart | No |
| Cart | `POST` | `/api/cart/{cartId}/items` | Add item to cart | No |
| Cart | `PUT` | `/api/cart/{cartId}/items/{productId}?quantity={quantity}` | Update cart item quantity | No |
| Cart | `DELETE` | `/api/cart/{cartId}/items/{productId}` | Remove item from cart | No |
| Cart | `DELETE` | `/api/cart/{cartId}` | Clear cart | No |
| Cart | `POST` | `/api/cart/merge?anonymousCartId={anonId}&userId={userId}` | Merge anonymous cart into user cart after login | No |
| Order | `POST` | `/api/orders` | Create order | Yes |
| Order | `GET` | `/api/orders/{orderId}` | Get order details | Yes |
| Order | `GET` | `/api/orders/number/{orderNumber}` | Get order details by order number | Yes |
| Order | `GET` | `/api/orders/user/{userId}?page=0&size=20` | Get user's order history | Yes |

## Product Catalog GraphQL

Product Service exposes product catalog operations through GraphQL.

Current service endpoint:

```text
POST /graphql
```

Important: API Gateway currently routes `/api/products/**` to Product Service, but Product Service GraphQL uses the default `/graphql` path. If the SPA must access products only through the gateway, add a gateway route for `/graphql` or `/api/products/graphql`.

| Area | Endpoint | Operation | Purpose |
|---|---|---|---|
| Product | `POST /graphql` | `product(productId)` | Get product details |
| Product | `POST /graphql` | `products(page, size, sortBy)` | List products |
| Product | `POST /graphql` | `productsByCategory(category, page, size)` | List products by category |
| Product | `POST /graphql` | `productsByStatus(status, page, size)` | List products by status |
| Product | `POST /graphql` | `searchProducts(keyword, category, minPrice, maxPrice, page, size)` | Search and filter products |

Example product search query:

```graphql
query SearchProducts(
  $keyword: String!
  $category: String
  $minPrice: BigDecimal
  $maxPrice: BigDecimal
  $page: Int
  $size: Int
) {
  searchProducts(
    keyword: $keyword
    category: $category
    minPrice: $minPrice
    maxPrice: $maxPrice
    page: $page
    size: $size
  ) {
    content {
      productId
      name
      description
      category
      brand
      price
      currency
      images
      tags
      status
    }
    page
    size
    totalElements
    totalPages
    last
  }
}
```

## Request Body Shapes

### Register

```json
{
  "email": "customer@example.com",
  "username": "customer",
  "password": "password123",
  "firstName": "Jane",
  "lastName": "Doe",
  "phone": "+905555555555"
}
```

### Login

```json
{
  "username": "customer",
  "password": "password123"
}
```

### Add Address

```json
{
  "addressType": "SHIPPING",
  "street": "123 Main St",
  "city": "Istanbul",
  "state": "TR",
  "postalCode": "34000",
  "country": "Turkey",
  "isDefault": true
}
```

### Add To Cart

```json
{
  "productId": "PROD-001",
  "productName": "Product Name",
  "unitPrice": 99.99,
  "quantity": 1,
  "imageUrl": "https://example.com/product.jpg"
}
```

### Create Order

```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "cartId": "00000000-0000-0000-0000-000000000000",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Product Name",
      "quantity": 1,
      "unitPrice": 99.99
    }
  ],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Istanbul",
    "state": "TR",
    "postalCode": "34000",
    "country": "Turkey"
  }
}
```

## Not Recommended For Customer SPA

These routes exist in code or gateway configuration, but they should not be used directly by a customer-facing SPA.

| Method | Endpoint | Reason |
|---:|---|---|
| `GET` | `/api/users` | Lists all users; admin-like endpoint |
| `GET` | `/api/users/email/{email}` | User lookup by email; admin/internal-like endpoint |
| `GET` | `/api/users/{userId}/validate` | Better suited for downstream service validation |
| `POST` | `/api/inventory/batch/import` | CSV stock import; admin/back-office operation |
| `GET` | `/api/orders` | Lists all orders; admin-like endpoint |
| `GET` | `/api/orders/reports/daily-sales` | Reporting endpoint |
| `GET` | `/api/orders/reports/status-distribution` | Reporting endpoint |
| `POST /graphql` | `createProduct`, `updateProduct`, `deleteProduct` | Product management mutations; admin/back-office operations |
| `/api/payments/**` | Gateway route exists, but Payment Service currently has no REST controller | Payment Service currently exposes gRPC, not customer REST |

## SPA Flow Summary

1. Use Product GraphQL queries for catalog, detail, category pages, and search.
2. Use `/api/cart/**` for anonymous and logged-in cart flows.
3. Use `/api/users/register` and `/api/users/login` for customer identity.
4. On login, call `/api/cart/merge` if an anonymous cart exists.
5. Use `/api/orders` to create orders and `/api/orders/user/{userId}` for order history.
