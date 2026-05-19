# API Gateway

This is the API Gateway for the FlowBoard microservices architecture. It acts as the single entry point for all client requests, routing them to the appropriate backend microservices. It also handles cross-cutting concerns like CORS and authentication propagation.

## Requirements
- Java 17+
- Maven

## How to Run Locally

You can run the API Gateway locally using Maven:

```bash
# Clean and compile the project
mvn clean install -DskipTests

# Run the Spring Boot application
mvn spring-boot:run
```

Alternatively, you can run it using the Maven wrapper included in the project:
```bash
./mvnw spring-boot:run
```

## Docker Deployment

To run the API Gateway via Docker (usually as part of the larger `docker-compose` setup):
```bash
docker-compose up -d api-gateway
```
