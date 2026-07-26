# project-scaffold
Basic Project Scaffold working through some general consideration for a starting a new site.

## MVN
`mvn -N wrapper:wrapper`

`./mvnw clean package`

`./mvnw spring-boot:run -q`

*This will fail due to the DB connections* 

## Start-up external services with docker
`cd src/main/docker`

`docker-compose up`

## Start-up Application
`./mvnw spring-boot:run`

## Postgres
`psql -U [USER] -d [DATABASE]`

Show tables
`\dt`

## Flyway
Run migrations for development
`./mvnw clean flyway:migrate -Dflyway.configFiles=flyway.conf`

## Health
Swagger Endpoint `/swagger-ui/index.html`
Actuator Health `/actuator/health/*`
- `/liveness` - UP/DOWN
- `/readiness` - components health ie DB/Redis