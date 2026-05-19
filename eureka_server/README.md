# Eureka Discovery Server

This is the Eureka Service Registry for the FlowBoard microservices architecture. It acts as a central registry where all other microservices register themselves, allowing them to dynamically discover and communicate with each other without hardcoded IP addresses or ports.

## Requirements
- Java 17+
- Maven

## How to Run Locally

You can run the Eureka Server locally using Maven:

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

## Accessing the Dashboard

Once the Eureka Server is started, its UI dashboard can typically be accessed at:
[http://localhost:8761](http://localhost:8761)

From the dashboard, you can monitor the status of all registered microservices (e.g., `user-service`, `workspace-service`, etc.), their instances, and their availability.

## Docker Deployment

To run the Eureka Server via Docker (usually as part of the larger `docker-compose` setup):
```bash
docker-compose up -d eureka-server
```
