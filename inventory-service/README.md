# Inventory Service

A Spring Boot REST API for managing product inventory with Redis caching and PostgreSQL persistence.

## Tech Stack

- **Java 25** / Spring Boot 4.1.2
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **Flyway** - Database migrations
- **Maven** - Build tool

## Prerequisites

- Docker & Docker Compose
- Java 25 (for local development without Docker)
- Maven 3.9+ (for local development without Docker)

---

## Quick Start with Docker

### Option 1: Using Docker Compose (Recommended)

Create a `docker-compose.yml` file in the project root:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: inventory-postgres
    environment:
      POSTGRES_DB: inventory
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: inventory-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: inventory-service
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: inventory
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ""
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy

volumes:
  postgres_data:
  redis_data:
```

### Option 2: Build and Run with Docker Commands

**1. Build the Docker image:**
```bash
# From the inventory-service directory
docker build -t inventory-service:latest .
```

**2. Run PostgreSQL:**
```bash
docker run -d \
  --name inventory-postgres \
  -e POSTGRES_DB=inventory \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine
```

**3. Run Redis:**
```bash
docker run -d \
  --name inventory-redis \
  -p 6379:6379 \
  -v redis_data:/data \
  redis:7-alpine
```

**4. Run the Application:**
```bash
docker run -d \
  --name inventory-service \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=inventory \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -p 8080:8080 \
  inventory-service:latest
```

> **Note:** On Linux, replace `host.docker.internal` with your host machine's IP address.

---

## Dockerfile

Create a `Dockerfile` in the `inventory-service` directory:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/inventory-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Verify the Service is Running

```bash
# Health check
curl http://localhost:8080/actuator/health

# Or check logs
docker logs -f inventory-service
```

---

## API Endpoints

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/products` | Create a new product |
| GET | `/api/v1/products/{id}` | Get product by ID |
| GET | `/api/v1/products` | List products (paginated) |
| PUT | `/api/v1/products/{id}` | Update product |
| DELETE | `/api/v1/products/{id}` | Delete product |

### Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/monitoring/redis/keys` | List Redis keys |
| GET | `/api/v1/monitoring/redis/stats` | Redis cache statistics |
| GET | `/api/v1/monitoring/redis/keys/{pattern}` | Get keys by pattern |
| GET | `/api/v1/monitoring/redis/value/{key}` | Get Redis value by key |

---

## Example API Calls

### Create a Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "Gaming Laptop",
    "price": 1299.99,
    "quantity": 10,
    "category": "Electronics"
  }'
```

### Get Product by ID
```bash
curl http://localhost:8080/api/v1/products/1
```

### List Products (with pagination)
```bash
curl "http://localhost:8080/api/v1/products?page=0&size=10&sort=name,asc"
```

### Update Product
```bash
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Pro",
    "description": "Professional Gaming Laptop",
    "price": 1499.99,
    "quantity": 5,
    "category": "Electronics"
  }'
```

### Delete Product
```bash
curl -X DELETE http://localhost:8080/api/v1/products/1
```

### Redis Monitoring
```bash
# Get all Redis keys
curl http://localhost:8080/api/v1/monitoring/redis/keys

# Get Redis statistics
curl http://localhost:8080/api/v1/monitoring/redis/stats

# Get keys matching pattern
curl http://localhost:8080/api/v1/monitoring/redis/keys/v1:products:*
```

---

## Local Development (Without Docker)

### Prerequisites
- Java 25
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+

### Setup

1. **Start PostgreSQL and Redis locally**
2. **Configure environment variables:**
   ```bash
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=inventory
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   export REDIS_PASSWORD=""
   ```
3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

---

## Running Tests

```bash
# Unit tests
./mvnw test

# With Docker (integration tests)
docker compose -f docker-compose.test.yml up --abort-on-container-exit
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `inventory` | Database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | - | Database password (required) |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | - | Redis password (optional) |

---

## Project Structure

```
inventory-service/
├── src/
│   ├── main/
│   │   ├── java/com/pranit/inventory/
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # Data access
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── exception/       # Exception handling
│   │   │   └── InventoryApplication.java
│   │   └── resources/
│   │       ├── application.yaml # Configuration
│   │       └── db/migration/    # Flyway migrations
│   └── test/
└── pom.xml
```

---

## Stopping Services

### Docker Compose
```bash
docker compose down
```

### Individual Containers
```bash
docker stop inventory-service inventory-postgres inventory-redis
docker rm inventory-service inventory-postgres inventory-redis

# Remove volumes (WARNING: deletes data)
docker volume rm postgres_data redis_data
```

---

## Troubleshooting

### Port Conflicts
If ports 5432, 6379, or 8080 are in use:
```bash
# Check what's using the port
lsof -i :8080
# Kill the process or change ports in docker-compose.yml
```

### Database Connection Issues
- Ensure PostgreSQL is healthy: `docker exec inventory-postgres pg_isready -U postgres`
- Check Flyway migration logs: `docker logs inventory-service`

### Redis Connection Issues
- Test Redis: `docker exec inventory-redis redis-cli ping`
- Check Redis logs: `docker logs inventory-redis`

### Application Logs
```bash
docker logs -f inventory-service --tail 100
```

---

## License

MIT License