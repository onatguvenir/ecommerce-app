---
type: ai-context
scope: domain-spec
service: user-service
port-http: 8081
port-grpc: 9081
db: PostgreSQL (userdb)
last-updated: 2026-04-27
---

# user-service Domain Spec

## Responsibility
User accounts, authentication, profile management. Exposes gRPC for user validation/lookup by other services.

## gRPC Server (port 9081)

### ValidateUser
```
ValidateUserRequest { userId: string }
ValidateUserResponse { isValid: bool, isActive: bool, message: string }
```
Called by order-service saga before processing order.

### GetUser
```
GetUserRequest { userId: string }
User { userId, email, firstName, lastName, phoneNumber, ... }
```
Called by notification-service to get contact info for notifications.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `UserGrpcServiceImpl` | `infrastructure/grpc/` | gRPC server implementation |
| `UserApplicationService` | `application/service/` | User CRUD, auth logic |
| `User` | `domain/model/` | User aggregate |
| `UserRepository` | `domain/repository/` | Interface |

## REST Endpoints
```
POST   /api/v1/auth/register     register user
POST   /api/v1/auth/login        login → JWT
GET    /api/v1/users/{id}        getUserById
PUT    /api/v1/users/{id}        updateUser
DELETE /api/v1/users/{id}        deleteUser
```

## JWT
User-service generates JWT on login. api-gateway validates JWT on all downstream requests.

## DB Tables
- `users` — user accounts

## Notes
- Other services identify users by UUID `userId` only.
- User profile data (email, name, phone) not replicated to other services — fetched on demand via gRPC.
