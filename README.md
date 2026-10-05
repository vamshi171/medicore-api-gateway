# medicore-api-gateway

**Spring Cloud Gateway** edge service for the **MediCore** healthcare platform
([monorepo](https://github.com/Vamshikrishna720/medicore)): single entry point for the
React frontend and external clients.

## Responsibilities

- **JWT validation at the edge** — per-route `Jwt` filter verifies the Bearer token (HS256) and forwards identity as `X-User-Id / X-User-Email / X-User-Role`
- **Role-based routing** — per-route `RoleAuth=ADMIN,DOCTOR,PATIENT` filters (e.g. `/api/auth/users/**` is ADMIN-only; `/api/patients` list is ADMIN-only)
- **Public routes** — `/api/auth/register`, `/api/auth/login`, doctor search (no filters attached)
- **CORS** for the React dev server (localhost:5173 / 3000)
- Load-balanced routing via Eureka (`lb://AUTH-SERVICE`, …)

## Run

> **Prerequisite:** this repo depends on `com.medicore:medicore-common:1.0.0`. Install it to your local Maven repo first — clone [medicore-common](https://github.com/Vamshikrishna720/medicore-common) and run `mvn clean install` there. CI has the same requirement (publishing common to GitHub Packages would make this repo fully self-contained).

```bash
mvn spring-boot:run          # gateway on :8080
```

| Env var | Default | Purpose |
|---|---|---|
| `EUREKA_URI` | http://localhost:8761/eureka | Service registry |
| `JWT_SECRET` | dev value | Must match auth-service |

> Reactive stack note: this service is WebFlux/Netty — `medicore-common`'s servlet
> starter is excluded in the POM to keep Boot on the reactive server.

## Routes (summary)

| Path | Service | Access |
|---|---|---|
| `/api/auth/register`, `/api/auth/login` | auth-service | public |
| `/api/auth/me`, `/api/auth/me/**` | auth-service | JWT |
| `/api/auth/users/**` | auth-service | ADMIN |
| `/api/patients` (+`/{id}/status`) | patient-service | ADMIN |
| `/api/patients/**` | patient-service | JWT (PATIENT/ADMIN) |
| `/api/doctors/me/**` | doctor-service | DOCTOR |
| `/api/doctors/**` | doctor-service | public |
| `/api/appointments/**` | appointment-service | JWT (all roles) |
| `/api/notifications/**` | notification-service | JWT (all roles) |
