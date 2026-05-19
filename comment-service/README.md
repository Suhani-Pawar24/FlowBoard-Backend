# Comment Service

This microservice handles user comments, discussions, and attachments associated with task cards in the FlowBoard platform.

## Requirements
- Java 17+
- Maven

## How to Run Locally

You can run the service locally using Maven:

```bash
# Clean and compile the project
mvn clean install -DskipTests

# Run the Spring Boot application
mvn spring-boot:run
```

## Docker Deployment

To run this service via Docker (as part of the larger `docker-compose` setup):
```bash
docker-compose up -d comment-service
```
