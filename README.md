# Finance Dashboard API

**Zorvyn FinTech Pvt. Ltd.** — Production-grade RESTful backend powering a multi-role finance dashboard with secure access control, analytics, and anomaly detection.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.3.5 |
| Language | Java 25 |
| Database | PostgreSQL 16 |
| Security | JWT (jjwt 0.12.3) + BCrypt (strength 12) |
| ORM | Hibernate 6.5 + Spring Data JPA |
| API Docs | SpringDoc OpenAPI 2.6.0 (Swagger UI) |
| Build | Maven 3.x |

---

## Quick Start

### Prerequisites

- **Java 25** or later
- **PostgreSQL 16** running on `localhost:5432`
- **Maven 3.x**

### 1. Create the Database

```bash
createdb finance_dashboard
```

### 2. Run the Application

```bash
# Standard startup (empty database)
mvn spring-boot:run

# With demo seed data (3 users, 21 transactions)
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

The API starts on `http://localhost:8080`.

### 3. Access Swagger UI

Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) in your browser.

Click **Authorize** and paste a JWT token obtained from the login endpoint.

---

## Seed Data Credentials

When running with the `seed` profile, the following test accounts are created:

| Role | Email | Password |
|------|-------|----------|
| ADMIN | `admin@zorvyn.com` | `Admin@1234` |
| ANALYST | `analyst@zorvyn.com` | `Analyst@1234` |
| VIEWER | `viewer@zorvyn.com` | `Viewer@1234` |

---

## API Endpoints

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/auth/register` | Register a new account |
| `POST` | `/api/v1/auth/login` | Login and receive JWT |

### Transactions (Authenticated)

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `POST` | `/api/v1/transactions` | ADMIN, ANALYST | Create transaction |
| `GET` | `/api/v1/transactions` | ALL | List with pagination, sorting, filtering |
| `GET` | `/api/v1/transactions/{id}` | ALL | Get by ID |
| `PUT` | `/api/v1/transactions/{id}` | ADMIN, ANALYST | Update (owner or ADMIN) |
| `DELETE` | `/api/v1/transactions/{id}` | ADMIN, ANALYST | Soft-delete (owner or ADMIN) |

### Dashboard (Authenticated)

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `GET` | `/api/v1/dashboard/summary` | ALL | Income, expense, net balance totals |
| `GET` | `/api/v1/dashboard/trends` | ALL | Monthly income/expense trends (12 months) |
| `GET` | `/api/v1/dashboard/categories` | ALL | Expense breakdown by category |

### Insights (Restricted)

| Method | Endpoint | Roles | Description |
|--------|----------|-------|-------------|
| `GET` | `/api/v1/insights` | ADMIN, ANALYST | Spending analytics and anomaly detection |

### User Management (Admin Only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/users` | List all users |
| `GET` | `/api/v1/users/{id}` | Get user by ID |
| `PATCH` | `/api/v1/users/{id}/role` | Update user role |
| `PATCH` | `/api/v1/users/{id}/deactivate` | Deactivate user |
| `PATCH` | `/api/v1/users/{id}/activate` | Activate user |

---

## Architecture

```
com.zorvyn.financedashboard
├── config/                  # Security, OpenAPI, CORS, DataSeeder
├── controller/              # REST controllers (zero business logic)
├── dto/
│   ├── request/             # Validated request DTOs
│   └── response/            # Response DTOs + ApiResponse envelope
├── exception/               # Custom exceptions + GlobalExceptionHandler
├── model/
│   └── enums/               # Role, UserStatus, TransactionType
├── repository/              # JPA repositories + TransactionSpecification
├── security/                # JWT filter, service, UserDetailsService
└── service/                 # Business logic layer
```

### Design Decisions

- **Stateless JWT Auth** — No server-side sessions. JWTs carry `userId`, `role`, and `name` claims.
- **BCrypt (strength 12)** — Intentionally slow hashing for password security.
- **Soft Deletes** — Transactions are marked `isDeleted=true`, never physically removed.
- **Ownership Enforcement** — Only the transaction creator or an ADMIN can modify/delete.
- **Self-Protection** — Admins cannot demote or deactivate themselves.
- **Anomaly Detection** — Flags spending when current month exceeds 150% of the 6-month trailing average.
- **Uniform Error Envelope** — All responses (success and error) use `ApiResponse<T>` with `success`, `message`, `data`, and `timestamp` fields.

### Error Handling

All errors return structured JSON — no HTML, no stack traces:

```json
{
  "success": false,
  "message": "Validation failed: email: Must be a valid email address",
  "timestamp": "2026-04-04T16:01:39.803231"
}
```

| HTTP Code | When |
|-----------|------|
| 400 | Validation errors, malformed JSON, type mismatches |
| 401 | Missing or expired JWT token |
| 403 | Insufficient role or ownership violation |
| 404 | Resource not found |
| 405 | Wrong HTTP method |
| 409 | Duplicate resource (e.g., email already registered) |
| 500 | Unexpected server error (stack trace logged, not exposed) |

---

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Server port |
| `jwt.secret-key` | Base64 key | HS256 signing key |
| `jwt.expiration-ms` | `86400000` | Token TTL (24 hours) |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema auto-migration |

---

## License

MIT © Zorvyn FinTech Pvt. Ltd.
