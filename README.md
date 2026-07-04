# project-scaffold
Basic Project Scaffold working through some general consideration for a starting a new site.

## MVN
`mvn -N wrapper:wrapper`
`./mvnw clean package`
`./mvnw spring-boot:run`
*This will fail due to the DB connections* 

## Start-up external services with docker
`cd src/main/docker`
`docker-compose up`

## Start-up Application
`./mvnw spring-boot:run`
