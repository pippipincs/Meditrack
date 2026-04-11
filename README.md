# Patient Management System

A microservices-based patient management platform built with Java and Spring Boot. The system handles patient CRUD operations, automated billing account creation, real-time event analytics, and JWT-based authentication — all orchestrated through an API gateway.

## Architecture

```
                         ┌──────────────┐
                         │  API Gateway │ :4004
                         └──────┬───────┘
                       ┌────────┼────────┐
                       │        │        │
               ┌───────▼──┐ ┌──▼─────┐  │
               │  Patient  │ │  Auth  │  │
               │  Service  │ │Service │  │
               │   :4000   │ │ :4005  │  │
               └──┬────┬───┘ └────────┘  │
          gRPC    │    │  Kafka           │
        ┌─────────┘    └────────┐        │
        │                       │        │
  ┌─────▼──────┐       ┌───────▼──────┐ │
  │  Billing   │       │  Analytics   │ │
  │  Service   │       │   Service    │ │
  │ :4001/9001 │       │    :4002     │ │
  └────────────┘       └──────────────┘ │
                                        │
                         ┌──────────────┘
                         │
                   ┌─────▼──────┐
                   │ PostgreSQL │
                   │   Kafka    │
                   └────────────┘
```

### Communication Flow (Patient Creation)

1. Client sends `POST /api/patients` through the **API Gateway** with a JWT Bearer token
2. Gateway validates the token via **Auth Service** and routes the request to **Patient Service**
3. Patient Service creates the patient in **PostgreSQL**
4. Patient Service calls **Billing Service** via **gRPC** to create a billing account
5. Patient Service publishes a **Protobuf-serialized event** to the Kafka `patient` topic
6. **Analytics Service** consumes the event from Kafka for processing

Updates and deletes are simple CRUD operations with no gRPC or Kafka side effects.

## Services

| Service | Port(s) | Description |
|---|---|---|
| **api-gateway** | 4004 | Spring Cloud Gateway (WebFlux). Routes and authenticates all inbound traffic |
| **patient-service** | 4000 | Core REST API for patient CRUD. gRPC client and Kafka producer |
| **auth-service** | 4005 | JWT-based authentication. Login and token validation endpoints |
| **billing-service** | 4001 (HTTP), 9001 (gRPC) | gRPC server that creates billing accounts for new patients |
| **analytics-service** | 4002 | Kafka consumer that processes patient lifecycle events |

## Features

- **Patient Management** — Full CRUD API for patient records (name, email, address, date of birth)
- **JWT Authentication** — Secure login with token-based auth (HMAC-signed, 60-hour expiry)
- **Automated Billing** — Billing accounts created automatically on patient registration via gRPC
- **Event-Driven Analytics** — Patient events streamed through Kafka with Protobuf serialization
- **API Gateway** — Single entry point with routing, path rewriting, and auth validation
- **Cloud Deployment** — AWS CDK infrastructure (ECS Fargate, RDS PostgreSQL, MSK Kafka, ALB)
- **API Documentation** — Swagger UI available per service via SpringDoc OpenAPI
- **Seed Data** — 14 patients pre-loaded on startup via `data.sql`

## Tech Stack

| Category | Technology |
|---|---|
| **Language** | Java 17 (patient-service), Java 21 (all others) |
| **Framework** | Spring Boot 3.x / 4.x, Spring Cloud Gateway |
| **Database** | PostgreSQL 17, H2 (local dev/test) |
| **Messaging** | Apache Kafka 3.8 |
| **RPC** | gRPC with Protocol Buffers |
| **Auth** | Spring Security, JJWT 0.12.6 |
| **Infra** | Docker, Docker Compose, AWS CDK 2.x |
| **Cloud** | AWS ECS Fargate, RDS, MSK, ALB, CloudMap, CloudWatch |
| **Build** | Maven (per-service wrapper) |
| **Testing** | JUnit 5, REST Assured (integration tests) |
| **Docs** | SpringDoc OpenAPI / Swagger UI |

## Getting Started

### Prerequisites

- Java 17+ (Java 21 recommended)
- Docker & Docker Compose
- Maven (or use the included `mvnw` wrapper)

### Run with Docker Compose

```bash
docker compose up --build
```

This starts all services along with PostgreSQL and Kafka. The API Gateway is exposed on **port 4004**.

### Run Individual Services Locally

```bash
cd <service-dir>
./mvnw spring-boot:run
```

### Build

```bash
cd <service-dir>
./mvnw clean package            # build with tests
./mvnw clean package -DskipTests  # build without tests
```

### Run Tests

```bash
# Unit tests (per service)
cd <service-dir>
./mvnw test

# Integration tests (requires running services)
cd integration-tests
mvn test
```

## API Endpoints

### Auth Service

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/login` | Authenticate with email/password, returns JWT |
| GET | `/auth/validate` | Validate a Bearer token |

### Patient Service

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/patients` | List all patients |
| POST | `/api/patients` | Create a new patient |
| PUT | `/api/patients/{id}` | Update a patient |
| DELETE | `/api/patients/{id}` | Delete a patient |

All patient endpoints require a valid JWT token in the `Authorization: Bearer <token>` header.

Swagger UI: `http://localhost:4000/swagger-ui/index.html`


