# Payment Service

This microservice is responsible for managing subscription tiers, billing, and integration with the Razorpay payment gateway for the FlowBoard platform.

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
docker-compose up -d payment-service
```
