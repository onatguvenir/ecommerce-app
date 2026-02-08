# API Gateway - Monat E-Commerce

## Overview
Spring Cloud Gateway serving as the single entry point for all microservices.

## Features
- **Request Routing** - Routes to all 7 microservices
- **JWT Authentication** - Validates tokens for protected routes
- **Rate Limiting** - IP-based rate limiting using Redis
- **CORS Configuration** - Global CORS for frontend integration
- **Request Logging** - Correlation ID for distributed tracing
- **Error Handling** - Centralized error responses

## Port
- **8080** - Gateway HTTP endpoint

## Routes

| Path | Target Service | Port | Authentication | Rate Limit |
|------|---------------|------|----------------|------------|
| `/api/users/**` | User Service | 8081 | Optional | 10/sec |
| `/api/products/**` | Product Service | 8082 | No | 20/sec |
| `/api/inventory/**` | Inventory Service | 8083 | **Required** | 10/sec |
| `/api/cart/**` | Cart Service | 8084 | No | 30/sec |
| `/api/orders/**` | Order Service | 8085 | **Required** | 10/sec |
| `/api/payments/**` | Payment Service | 8086 | **Required** | 5/sec |

## Authentication
Protected routes require JWT token in Authorization header:
```
Authorization: Bearer <jwt-token>
```

## Rate Limiting
- Implemented using Redis
- IP-based rate limiting
- Different limits per service
- Returns HTTP 429 when exceeded

## Examples

### Public Access (No Auth)
```bash
# Get products
curl http://localhost:8080/api/products

# Search products
curl http://localhost:8080/api/products/search?keyword=laptop

# Get cart
curl http://localhost:8080/api/cart/session-123
```

### Protected Access (Auth Required)
```bash
# Create order (requires JWT)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{...}'

# Get user orders
curl http://localhost:8080/api/orders/user/{userId} \
  -H "Authorization: Bearer <jwt-token>"
```

## Configuration
See `application.yml` for:
- Route definitions
- Rate limit configuration
- JWT secret (change in production!)
- CORS settings

## Monitoring
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus
- Gateway routes: http://localhost:8080/actuator/gateway/routes
