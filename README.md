# Resource Booking System

## Description

A secure RESTful Resource Booking System built with Spring Boot 3.x. This application allows users to book resources (rooms, vehicles, equipment) with role-based access control, JWT authentication, and comprehensive reservation management features.

## Features

- **JWT Authentication** - Secure stateless authentication with BCrypt password hashing
- **Role-Based Access Control (RBAC)** - ADMIN and USER roles with distinct permissions
- **Resource CRUD** - Full resource management for ADMINs
- **Reservation Management** - Create, read, update, delete, and cancel reservations
- **Ownership Protection** - Users can only access their own reservations
- **Filtering** - Filter reservations by status, price range
- **Pagination** - Configurable page size with metadata
- **Sorting** - Sort by price, startTime, endTime, createdAt, status
- **Conflict Detection** - Prevents double-booking of resources
- **Swagger/OpenAPI** - Interactive API documentation
- **PostgreSQL** - Production-ready database
- **Comprehensive Testing** - Unit and integration tests with H2

## Tech Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **Spring Security 6**
- **Spring Data JPA / Hibernate**
- **PostgreSQL** (production) / **H2** (tests)
- **JWT** (io.jsonwebtoken 0.12.3)
- **MapStruct** (DTO mapping)
- **Lombok** (boilerplate reduction)
- **Jakarta Bean Validation**
- **springdoc-openapi** (Swagger UI)
- **JUnit 5** / **MockMvc** / **Spring Security Test**
- **Maven**

## Requirements

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (for production)

## Environment Variables

Create a `.env` file or set the following environment variables:

```env
DB_URL=jdbc:postgresql://localhost:5432/resource_booking
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=your-base64-encoded-secret-key-at-least-32-chars
JWT_EXPIRATION_MS=86400000
```

### JWT Secret Generation

Generate a secure base64-encoded secret:

```bash
openssl rand -base64 32
```

## Database Setup

### PostgreSQL

```sql
CREATE DATABASE resource_booking;
CREATE USER postgres WITH ENCRYPTED PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE resource_booking TO postgres;
```

The application uses `ddl-auto: update` for automatic schema management.

## Running Locally

### Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Using Maven

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Running Tests

```bash
./mvnw test
```

Or:

```bash
mvn test
```

## Building

```bash
./mvnw clean package
```

The JAR will be in `target/resource-booking-system-1.0.0.jar`

## Seed Accounts

The application automatically seeds the following users on startup (if not already present):

| Role | Username | Password |
|------|----------|----------|
| ADMIN | admin | Admin@123 |
| USER | user | User@123 |

**Note:** These are development/test credentials only. Change them in production!

### Seeded Resources

Four sample resources are also created:
- Conference Room A (ROOM) - ₹500/hour
- Conference Room B (ROOM) - ₹800/hour
- Company Vehicle (VEHICLE) - ₹300/hour
- Projector Set (EQUIPMENT) - ₹150/hour

## Authentication

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "User@123"
}
```

### Response

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

### Using the Token

Include the token in the Authorization header for all protected endpoints:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## API Endpoints

### Authentication

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/auth/login` | Public | Authenticate and get JWT |

### Resources

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/resources` | USER, ADMIN | List all resources (paginated) |
| GET | `/resources/{id}` | USER, ADMIN | Get resource by ID |
| POST | `/resources` | ADMIN | Create new resource |
| PUT | `/resources/{id}` | ADMIN | Update resource |
| DELETE | `/resources/{id}` | ADMIN | Delete resource |

### Reservations

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/reservations` | USER, ADMIN | Create reservation |
| GET | `/reservations` | USER, ADMIN | List reservations (filtered, paginated, sorted) |
| GET | `/reservations/{id}` | USER, ADMIN | Get reservation by ID |
| PUT | `/reservations/{id}` | USER, ADMIN | Update reservation |
| DELETE | `/reservations/{id}` | USER, ADMIN | Delete reservation |
| PUT | `/reservations/{id}/cancel` | USER, ADMIN | Cancel reservation |

## Reservation Request Example

```json
{
  "resourceId": 1,
  "startTime": "2026-09-20T10:00:00",
  "endTime": "2026-09-20T12:00:00"
}
```

**Note:** No `userId` in request - ownership is determined from JWT.

## Filtering Examples

```http
# Filter by status
GET /reservations?status=CONFIRMED

# Filter by price range
GET /reservations?minPrice=100&maxPrice=5000

# Combined filters
GET /reservations?status=PENDING&minPrice=500&maxPrice=2000

# With pagination and sorting
GET /reservations?status=CONFIRMED&minPrice=100&maxPrice=5000&page=0&size=10&sort=price,asc
```

### Query Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | PENDING, CONFIRMED, CANCELLED |
| `minPrice` | decimal | Minimum price (inclusive) |
| `maxPrice` | decimal | Maximum price (inclusive) |
| `page` | int | Page number (0-based, default: 0) |
| `size` | int | Page size (max: 100, default: 10) |
| `sort` | string | Field,direction (e.g., `price,asc`, `startTime,desc`) |

### Allowed Sort Fields

- `price`
- `startTime`
- `endTime`
- `createdAt`
- `status`

Default sort: `createdAt,desc`

## Pagination Response

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 35,
  "totalPages": 4,
  "last": false,
  "first": true
}
```

## Swagger UI

Access the interactive API documentation at:

```
http://localhost:8080/swagger-ui/index.html
```

1. Call `POST /auth/login` to get a token
2. Click **Authorize** in Swagger UI
3. Enter `Bearer <your-token>`
4. Test protected endpoints

## Error Responses

All errors follow a consistent format:

```json
{
  "timestamp": "2026-09-09T19:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "End time must be after start time",
  "path": "/reservations"
}
```

### Common HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/missing token) |
| 403 | Forbidden (insufficient permissions/ownership) |
| 404 | Not Found |
| 409 | Conflict (double booking) |
| 500 | Internal Server Error |

## Assumptions / Design Decisions

1. **Price Calculation**: Reservation price is calculated server-side as `resource.pricePerHour × duration in hours`. Duration is calculated in minutes and converted to decimal hours with `HALF_UP` rounding to 2 decimal places.

2. **Cancelled Bookings**: CANCELLED reservations do not block time slots - they are excluded from conflict detection.

3. **Ownership**: User identity comes exclusively from JWT token. Request body never contains userId for reservation creation.

4. **Page Numbering**: Starts at 0 (Spring Data convention).

5. **Max Page Size**: Limited to 100 to prevent excessive loads.

6. **Time Validation**: Start time must be in the future; end time must be strictly after start time.

7. **Conflict Detection**: Two reservations overlap when `existing.start < requested.end AND existing.end > requested.start`. Cancelled reservations are ignored.

8. **Soft Delete**: Reservations are hard-deleted. Cancellation sets status to CANCELLED.

9. **Time Zones**: All times use LocalDateTime (no timezone). Assumes server and clients share timezone.

10. **Enum Serialization**: Enums serialized as strings (e.g., "PENDING", "ADMIN").

11. **JWT Expiration**: Default 24 hours (86400000 ms).

12. **Password Encoding**: BCrypt with default strength (10).

## Testing

The test suite covers:

- **Authentication**: Valid/invalid login, token validation
- **Resource RBAC**: USER read-only, ADMIN full CRUD
- **Reservation Security**: Ownership enforcement, admin access
- **Validation**: Required fields, time ranges, price validation
- **Booking Behavior**: Overlap detection, cancellation freeing slots
- **Filtering**: Status, min/max price, combined filters
- **Pagination/Sorting**: Page size, sort fields and directions

Run tests with H2 in-memory database for isolation.

## Future Improvements

- [ ] Refresh token mechanism
- [ ] Email notifications for booking confirmations
- [ ] Resource availability calendar endpoint
- [ ] Recurring reservations
- [ ] Audit logging
- [ ] Rate limiting
- [ ] Docker support with docker-compose
- [ ] Flyway migrations for production schema management
- [ ] Resource categories/tags
- [ ] Waitlist for fully booked resources

## License

This project is created for educational/assignment purposes.