# Docker Development Environment

This directory contains the Docker Compose configuration for the ecommerce application development environment.

## Services

- **PostgreSQL 15**: Primary database
- **Redis 7**: Cache and session storage
- **Garage**: S3-compatible object storage for uploaded images (e.g. category thumbnails/panel images).

## Quick Start

### Option 1: External Services Only (Recommended for Development)

Start only PostgreSQL, Redis, and Garage (run application from IDE):

```bash
cd docker
docker-compose up -d
./garage-init.sh
```

The application can then be run from your IDE with the `docker` profile:
```
--spring.profiles.active=docker
```

### Option 2: Full Application Stack

Run everything including the application container:

```bash
cd docker
docker-compose -f docker-compose.yml -f app-docker-compose.yml up -d
./garage-init.sh
```

`garage-init.sh` bootstraps Garage's single-node layout, bucket, and a fixed access/secret key pair. The `dxflrs/garage` image ships as a single static binary with no shell, so this can't run as a compose service - the script drives the already-running container from the host via `docker exec` instead. It's idempotent, so it's safe to run again any time (e.g. after `docker-compose down -v` wipes the volumes).

## Management Commands

- Stop services:
  ```bash
  docker-compose down
  ```

- View logs:
  ```bash
  docker-compose logs -f
  ```

- Restart specific service:
  ```bash
  docker-compose restart postgres
  ```

- Clean up (removes volumes):
  ```bash
  docker-compose down -v
  ```

## Configuration

Environment variables can be customized in the `.env` file:

### Database Settings
- `POSTGRES_DB`: Database name (default: ecommerce)
- `POSTGRES_USER`: Database user (default: ecommerce_user)  
- `POSTGRES_PASSWORD`: Database password (default: ecommerce_pass)
- `POSTGRES_PORT`: Database port (default: 5432)

### Redis Settings
- `REDIS_PASSWORD`: Redis password (default: redis_pass)
- `REDIS_PORT`: Redis port (default: 6379)

### Garage Settings
- `GARAGE_ACCESS_KEY` / `GARAGE_SECRET_KEY`: S3 credentials `garage-init.sh` provisions for the app (fixed values, so they don't change between restarts)
- `GARAGE_BUCKET`: Bucket `garage-init.sh` creates for uploaded images (default: category-images)
- `GARAGE_S3_PORT`: Port the S3 API listens on (default: 3900)
- `GARAGE_ADMIN_PORT`: Port the admin API listens on (default: 3903)

### Application Settings
- `JWT_SECRET`: JWT signing secret
- `JWT_EXPIRATION`: JWT expiration time in milliseconds
- Rate limiting and connection pool settings

## Application Profiles

The application includes two configuration profiles:

1. **Default Profile** (`application.yaml`): For local development with localhost services
2. **Docker Profile** (`application-docker.yaml`): For containerized development with Docker service names

## Data Persistence

- PostgreSQL data is persisted in the `postgres_data` Docker volume
- Redis data is persisted in the `redis_data` Docker volume

## Health Checks

Both services include health checks to ensure they're ready before the application starts:
- PostgreSQL: `pg_isready` check
- Redis: Connection test

## Networking

Services communicate via the `ecommerce-network` bridge network on subnet 172.20.0.0/16.

## Troubleshooting

- Check service logs: `docker-compose logs [service_name]`
- Verify network connectivity: `docker network ls`
- Test database connection: `docker-compose exec postgres psql -U ecommerce_user -d ecommerce`
- Test Redis connection: `docker-compose exec redis redis-cli -a redis_pass ping`