# CinePremier Backend

CinePremier is a microservice backend for cinema catalogue, identity, booking, payment, and movie recommendations.

## Services

| Service | Purpose | Container port |
| --- | --- | --- |
| API Gateway | The only public entry point; routes APIs and aggregates OpenAPI | 8080 |
| Identity | Authentication, users, roles, and seeded accounts | 8081 (internal) |
| Catalog | Movies, cinemas, rooms, seats, showtimes, and pricing | 8082 (internal) |
| Booking | Reservations, food orders, check-in, and reports | 8083 (internal) |
| Payment | Payments, wallet, loyalty, and refunds | 8084 (internal) |
| Recommendation | FastAPI recommendation service | 8000 (internal) |

Each stateful service has its own PostgreSQL database. RabbitMQ is shared by the services that publish events. PostgreSQL, RabbitMQ, and downstream services are private to the Docker network; only the Gateway is published to the host on port `8080` by default.

## Start locally with Docker

Prerequisite: Docker Desktop must be running.

```powershell
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

If port `8080` is already used on the host, choose another host port without exposing any downstream service:

```powershell
$env:GATEWAY_PORT = 8081
docker compose up --build -d
```

Use `http://localhost:$env:GATEWAY_PORT` for the health and Swagger URLs in that shell.

The first build downloads Maven and Python dependencies and can take several minutes. Inspect startup logs when a service is unhealthy:

```powershell
docker compose logs --tail=200 api-gateway
docker compose logs --tail=200 identity-service
```

Verify the public entry point:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Swagger/OpenAPI aggregation is available through the gateway at `http://localhost:8080/swagger-ui.html`. The individual API specifications are routed as `/v3/api-docs/identity`, `/catalog`, `/booking`, `/payment`, and `/recommendation`.

Stop the stack without removing development data:

```powershell
docker compose down
```

To delete all local development database and RabbitMQ data as well, use `docker compose down -v`. This operation is destructive.

## Configuration

Every service has a tracked `.env.example` file. Docker Compose loads it first, then loads an untracked service-specific `.env` file when present. Copy an example before replacing placeholder credentials or third-party keys:

```powershell
Copy-Item cinema-services/identity-service/.env.example cinema-services/identity-service/.env
```

The Compose file always overrides database hosts, RabbitMQ hosts, and service URLs so they use internal Docker DNS names. Do not put real secrets in `.env.example`; keep them only in ignored `.env` files or in deployment secret management.

For a real deployment, replace the example JWT and internal-service secrets, configure email/OAuth/payment credentials, and use non-default database passwords.

## Development seed accounts

Identity creates these accounts on an empty local database:

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@cinemaai.com` | `Admin123` |
| Manager | `manager@cinemaai.com` | `Admin123` |
| Staff | `staff@cinemaai.com` | `Staff123@` |

They are local-development defaults and must be changed outside development.

## Useful diagnostics

```powershell
# Service status and health checks
docker compose ps

# View the migration state without publishing the database port
docker compose exec catalog-db psql -U catalog_user -d catalog_db -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"

# Confirm the host only sees the Gateway port
docker compose ps --format json
```
