# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java microservices system for patient management consisting of four independent Spring Boot services. There is no parent POM or mono-repo build — each service is built independently.

## Services

| Service | Port | Role |
|---|---|---|
| **patient-service** | 4000 | Core REST API, gRPC client, Kafka producer. Java 17 / Spring Boot 3.5.5 |
| **billing-service** | 4001 (HTTP), 9001 (gRPC) | gRPC server for billing account creation. Java 21 / Spring Boot 4.0.3 |
| **analytics-service** | 4002 | Kafka consumer that processes patient events. Java 21 / Spring Boot 4.0.4 |
| **api-gateway** | 4004 | Spring Cloud Gateway (WebFlux). Routes `/api/patients/**` → patient-service. Java 21 / Spring Boot 4.0.5 |

## Build Commands

Each service uses Maven with a wrapper. Run from the service directory:

```bash
cd <service-dir>
./mvnw clean package          # build
./mvnw clean package -DskipTests  # build without tests
./mvnw test                   # run tests
./mvnw spring-boot:run        # run locally
```

Docker build per service:
```bash
docker build -t <service-name> ./<service-dir>
```

## Architecture

### Communication Flow (patient creation)

1. Client → `POST /patients` on patient-service (REST)
2. patient-service → billing-service via **gRPC** (`BillingService.CreateBillingAccount`, blocking stub on port 9001)
3. patient-service → Kafka topic `patient` via **Protobuf-serialized** `PatientEvent`
4. analytics-service consumes from Kafka topic `patient` (group: `analytics-service`)

Updates and deletes do NOT trigger gRPC or Kafka side effects.

### Protobuf

Proto files are **duplicated** across services (no shared library):
- `billing_service.proto` — in both `billing-service/src/proto/` and `patient-service/src/main/proto/`
- `patient_event.proto` — in both `patient-service/src/main/proto/` and `analytics-service/src/main/proto/`

Java stubs are generated at build time via `protobuf-maven-plugin`. When changing a proto, update it in all services that carry a copy.

### Data Layer (patient-service only)

- **Production**: PostgreSQL
- **Local dev**: H2 in-memory (commented out in `application.properties`, uncomment to use)
- Entity: `Patient` (UUID PK, name, email [unique], address, dateOfBirth, registeredDate)
- `data.sql` seeds 14 patients on startup

### API Gateway Routing

- `GET/POST/PUT/DELETE /api/patients/**` → `http://patient-service:4000` (strips `/api` prefix)
- `GET /api-docs/patients` → patient-service OpenAPI docs

### Key Configuration Properties

- `billing.service.address` / `billing.service.grpc.port` — gRPC client target in patient-service
- Kafka bootstrap servers — configured in patient-service (producer) and analytics-service (consumer)
- Swagger UI available at `http://localhost:4000/swagger-ui/index.html`

## Testing

Only boilerplate `@SpringBootTest` context-load tests exist. No unit or integration tests yet. The patient-service test context expects H2 on the classpath.

## Known Issues

- `analytics-service/src/main/resources/application.properties` has a typo: `spring.kafka.comsumer` should be `spring.kafka.consumer`
- Spring Boot / Java version mismatch: patient-service uses Java 17 + Boot 3.x; the other three services use Java 21 + Boot 4.x
