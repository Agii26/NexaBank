# NexaBank 🏦

A full-stack banking system built as a **TESDA NC III Programming** capstone project.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)

---

## Features

- **JWT Authentication** — stateless login/register with 15-min access tokens + 7-day refresh tokens
- **Role-based Access Control** — CUSTOMER, TELLER, and ADMIN roles
- **Bank Accounts** — open SAVINGS or CHECKING accounts with auto-generated account numbers
- **Transactions** — deposit, withdraw, and atomic fund transfers (either fully completes or fully rolls back)
- **PDF Statements** — downloadable account statements with date-range filtering via iText 8
- **Admin Panel** — view all users and accounts, freeze/unfreeze accounts
- **Audit Logging** — every sensitive action recorded with timestamp and actor
- **Paginated API** — all list endpoints support `?page=&size=` parameters
- **Swagger UI** — self-documenting API at `/swagger-ui.html`

---

## Architecture

```
React + Vite  (Axios · React Router · Zustand)
      ↓  HTTP / REST
Spring Security + JWT  (stateless · RBAC · CORS)
      ↓
REST Controllers  (Bean Validation · DTOs)
      ↓
Service Layer  (@Transactional · business rules)
      ↓
Repository Layer  (Spring Data JPA)
      ↓
MySQL 8  (5 tables · HikariCP)
```

### Database schema

| Table | Purpose |
|---|---|
| `users` | Auth credentials, role, active flag |
| `accounts` | Bank accounts (SAVINGS/CHECKING), balance, status |
| `transactions` | Every money movement with `balance_after` snapshot |
| `refresh_tokens` | SHA-256 hashed refresh tokens, single-session model |
| `audit_logs` | Who did what and when — every sensitive action |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.4.4, Spring Security 6.4, Spring Data JPA |
| Auth | JWT (jjwt 0.12.6), BCrypt password hashing |
| Database | MySQL 8, Hibernate 6.6, HikariCP |
| PDF | iText 8.0.3 |
| API Docs | Springdoc OpenAPI 2.7 (Swagger UI) |
| Frontend | React 18, Vite 5, Tailwind CSS 3 |
| State | Zustand (persisted auth store) |
| HTTP | Axios with JWT interceptor + auto-refresh on 401 |
| Routing | React Router v6 |
| Icons | Lucide React |

---

## Getting Started

### Prerequisites

- Java 17+ (Eclipse Temurin recommended)
- Maven 3.8+
- MySQL 8
- Node.js 18+

### 1. Clone the repository

```bash
git clone https://github.com/Agii26/NexaBank.git
cd NexaBank
```

### 2. Set up the database

```sql
CREATE DATABASE nexabank_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure database credentials

Edit `nexabank/src/main/resources/application-dev.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password_here
```

### 4. Start the backend

```bash
cd nexabank
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.  
Hibernate auto-creates all 5 tables on first run.

On startup you will see:

```
✓ Default admin created  →  admin@nexabank.com / Admin@123456
```

### 5. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The app opens at **http://localhost:5173**.  
The Vite proxy forwards all `/api` calls to the Spring Boot backend — no CORS setup needed in dev.

---

## Default Credentials

| Role | Email | Password |
|---|---|---|
| Admin | `admin@nexabank.com` | `Admin@123456` |
| Customer | Register at `/register` | (your choice, min 8 chars) |

> ⚠️ Change the admin password before deploying to production.

---

## API Endpoints

All endpoints require `Authorization: Bearer <token>` except auth routes.  
Full interactive docs at **http://localhost:8080/swagger-ui.html**.

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new customer |
| POST | `/api/auth/login` | Login, receive JWT tokens |
| POST | `/api/auth/refresh` | Exchange refresh token for new access token |

### Accounts
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts` | Open new account (SAVINGS/CHECKING) |
| GET | `/api/accounts` | List my accounts |
| GET | `/api/accounts/{id}` | Get account by ID |

### Transactions
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/transactions/deposit` | Deposit funds |
| POST | `/api/transactions/withdraw` | Withdraw funds |
| POST | `/api/transactions/transfer` | Transfer to another account by account number |
| GET | `/api/transactions/{accountId}/history` | Paginated transaction history |

### Statements
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/statements/{accountId}?from=YYYY-MM-DD&to=YYYY-MM-DD` | Download PDF statement |

### Admin (ADMIN role only)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/users` | List all users |
| GET | `/api/admin/accounts` | List all accounts |
| PATCH | `/api/admin/accounts/{id}/freeze` | Freeze an account |
| PATCH | `/api/admin/accounts/{id}/unfreeze` | Unfreeze an account |

---

## Project Structure

```
NexaBank/
├── nexabank/                          # Spring Boot backend
│   └── src/main/java/com/nexabank/
│       ├── config/                    # SecurityConfig, DataInitializer, OpenApiConfig
│       ├── controller/                # REST controllers
│       ├── dto/                       # Request/response DTOs
│       │   ├── request/
│       │   └── response/
│       ├── exception/                 # Exception hierarchy + GlobalExceptionHandler
│       ├── model/                     # JPA entities + enums
│       ├── repository/                # Spring Data JPA interfaces
│       ├── security/                  # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
│       └── service/                   # Business logic
└── frontend/                          # React frontend
    └── src/
        ├── api/                       # Axios API layer
        ├── components/                # Layout, AccountCard, TransactionRow, etc.
        ├── pages/                     # LoginPage, Dashboard, Accounts, Transfer, History, Admin
        └── store/                     # Zustand auth store
```

---

## Key Design Decisions

- **`DECIMAL(19,4)` for money** — never `float` or `double` for financial values
- **`balance_after` snapshot** — stored per transaction for instant statement generation without replaying history
- **Atomic transfers** — `@Transactional` ensures both debit and credit either fully commit or fully roll back
- **Single-session refresh tokens** — new login revokes all previous tokens for that user
- **SHA-256 hashed refresh tokens** — raw tokens never stored in the database
- **Async audit logging** — `@Async` + `Propagation.REQUIRES_NEW` so audit logs are always saved even if the business transaction rolls back
- **DTOs always** — JPA entities are never exposed directly in API responses

---

## Running Tests

```bash
cd nexabank
mvn test
```

Tests cover: `AuthService`, `AccountService`, `TransactionService` (11 unit tests using JUnit 5 + Mockito).

---

## Built with ❤️ in the Philippines 🇵🇭

Built by **Christian Benzon** as a TESDA NC III Programming capstone and portfolio project.
