# FlowBoard Backend Services - API Gateway & Eureka Server

## Overview

FlowBoard Backend is built on a microservices architecture with two critical components that work together to enable service discovery and request routing:

### **Eureka Server**
Eureka Server is a Netflix-based service registry that maintains a real-time database of available microservices. It provides dynamic service registration and discovery capabilities, allowing services to dynamically register/deregister themselves and enabling the API Gateway to discover and load-balance across service instances automatically.

### **API Gateway**
The API Gateway is the single entry point for all client requests. It routes incoming requests to appropriate backend services, handles cross-cutting concerns like authentication, rate limiting, and request/response transformations. It integrates with Eureka Server for dynamic service discovery and provides reactive, non-blocking request handling.

## System Architecture

```
┌────────────────────────────────────────────────────────┐
│              Client Applications                       │
│           (Web, Mobile, Desktop, etc.)                 │
└───────────────────┬────────────────────────────────────┘
                    │ All Requests
                    ▼
        ┌───────────────────────────┐
        │     API Gateway (8080)    │
        │  - Route Management       │
        │  - Rate Limiting          │
        │  - Load Balancing         │
        │  - Service Discovery      │
        └──┬──────────┬─────────────┘
           │          │ Queries service
           │          │ locations & health
           │          ▼
           │  ┌──────────────────┐
           │  │ Eureka Server    │
           │  │ (8761)           │
           │  │ - Service Registry
           │  │ - Instance Info   │
           │  │ - Health Status   │
           │  └──────────────────┘
           │
      ┌────┴──────┬─────────┬──────────┐
      │           │         │          │
    ┌─▼──┐    ┌──▼──┐  ┌──▼──┐   ┌───▼──┐
    │Svc │    │Svc  │  │Svc  │   │Svc   │
    │ A  │    │ B   │  │ C   │   │ D    │
    │8081│    │8082 │  │8083 │   │8084  │
    └────┘    └─────┘  └─────┘   └──────┘
    (Registered with Eureka)
```

---

## Table of Contents

1. [Eureka Server](#eureka-server)
2. [API Gateway](#api-gateway)
3. [Getting Started](#getting-started)
4. [Running Both Services](#running-both-services)
5. [Service Configuration](#service-configuration)
6. [Advanced Topics](#advanced-topics)

---

# EUREKA SERVER

## Features

- **Service Registration & Discovery**: Automatic registration and discovery of microservices
- **Health Monitoring**: Real-time health status checks for registered services
- **Load Balancing Support**: Enable client-side load balancing across service instances
- **Dashboard**: Web-based UI to monitor registered services and their instances
- **Highly Available**: Can be configured for multiple server instances in clustered mode
- **Self-Preservation**: Protects against false evictions when network issues occur

## Technology Stack

- **Java Version**: 17
- **Spring Boot**: 3.2.5
- **Spring Cloud**: 2023.0.1
- **Spring Cloud Netflix Eureka Server**: Service discovery and registration

## Eureka Server Configuration

### Default Configuration (application.yml)

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false      # Server doesn't register with itself
    fetch-registry: false            # Server doesn't fetch registry
```

### Key Properties

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | 8761 | Port on which Eureka Server runs |
| `spring.application.name` | eureka-server | Application identifier in service registry |
| `eureka.client.register-with-eureka` | false | Prevents server from registering with itself |
| `eureka.client.fetch-registry` | false | Prevents server from fetching registry |

## Eureka Server REST Endpoints

### Dashboard

Access the Eureka Server Dashboard to view registered services and their instances:

```
http://localhost:8761/
```

### REST API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/eureka/apps` | GET | Get all registered applications |
| `/eureka/apps/{appName}` | GET | Get specific application instances |
| `/eureka/apps/{appName}/{instanceId}` | GET | Get specific instance |
| `/eureka/apps/{appName}/{instanceId}` | PUT | Send heartbeat/renew lease |
| `/eureka/apps/{appName}/{instanceId}` | DELETE | Deregister instance |

### Example: Get All Registered Apps

```bash
curl -X GET http://localhost:8761/eureka/apps \
  -H "Accept: application/json"
```

## Health Check

The Eureka Server provides a health endpoint:

```bash
curl http://localhost:8761/health
```

---

# API GATEWAY

## Features

- **Request Routing**: Route requests to appropriate microservices based on paths and patterns
- **Dynamic Service Discovery**: Automatic service discovery using Eureka for load balancing
- **Reactive & Non-Blocking**: Built on Spring WebFlux for high-performance async request handling
- **Route Configuration**: Easy configuration of routes via application.yml
- **Load Balancing**: Distribute traffic across multiple service instances
- **Circuit Breaking**: Integration-ready for resilience patterns
- **Request/Response Transformation**: Support for modifying headers, paths, and request bodies
- **Health Monitoring**: Integrated health checks for gateway and upstream services

## Technology Stack

- **Java Version**: 17
- **Spring Boot**: 4.0.5
- **Spring Cloud**: 2025.1.1
- **Spring Cloud Gateway Server WebFlux**: Reactive gateway implementation
- **Spring Cloud Netflix Eureka Client**: Service discovery and registration
- **Project Reactor**: Reactive programming framework

## API Gateway Configuration

### Default Configuration (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway

  cloud:
    gateway:
      routes:
        - id: test-service
          uri: http://localhost:8081
          predicates:
            - Path=/test/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Key Properties

| Property | Value | Description |
|----------|-------|-------------|
| `server.port` | 8080 | Port on which API Gateway runs |
| `spring.application.name` | api-gateway | Application identifier in service registry |
| `spring.cloud.gateway.routes` | Array | Array of route configurations |
| `eureka.client.service-url.defaultZone` | http://localhost:8761/eureka/ | Eureka Server URL for service discovery |

## Routing Configuration

### Route Structure

A route in Spring Cloud Gateway consists of:

- **ID**: Unique identifier for the route
- **URI**: Destination where the request is routed
- **Predicates**: Conditions for matching requests
- **Filters**: Transformations applied to requests/responses

### Basic Route Examples

#### Static Route (Direct URL)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: test-service
          uri: http://localhost:8081
          predicates:
            - Path=/test/**
```

#### Dynamic Route (Service Discovery)
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service    # Load balanced to eureka registered service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=2          # Remove /api/users from path before routing
```

### Common Predicates

```yaml
predicates:
  - Path=/api/**                        # Match by path pattern
  - Method=GET,POST                     # Match by HTTP method
  - Host=example.com                    # Match by hostname
  - Header=X-Request-Id                 # Match by header presence
  - Query=token                         # Match by query parameter
  - After=2025-01-01T00:00:00Z         # Match after timestamp
```

### Common Filters

```yaml
filters:
  - StripPrefix=1                       # Remove n segments from path
  - AddRequestHeader=X-Gateway,FlowBoard  # Add header to request
  - AddResponseHeader=X-Gateway,FlowBoard # Add header to response
  - PrefixPath=/api                     # Add prefix to path
  - SetPath=/api${request.path}         # Set path with expression
```

## Service Discovery with Eureka

The API Gateway automatically discovers services registered with Eureka. Use `lb://` (load-balanced) prefix to route to services:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
        
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
        
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
```

**Important**: Service names must match the `spring.application.name` configured in each service.

## API Gateway Endpoints

### Gateway Health

```bash
curl http://localhost:8080/actuator/health
```

### Gateway Info

```bash
curl http://localhost:8080/actuator/info
```

### Available Routes (Actuator)

```bash
curl http://localhost:8080/actuator/gateway/routes
```

### Example Request through Gateway

If you have a route configured:

```bash
curl http://localhost:8080/test/hello
```

This would be routed to the configured upstream service.

---

# GETTING STARTED

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Git
- Ports 8080 and 8761 should be available

## Project Structure

```
FlowBoard-Backend/
├── eureka_server/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/flowboard/eureka_server/
│   │   │   │   └── EurekaServerApplication.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── application.yml
│   │   └── test/
│   ├── pom.xml
│   ├── mvnw & mvnw.cmd
│   └── README.md
│
├── api-gateway/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/flowboard/api_gateway/
│   │   │   │   └── ApiGatewayApplication.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── application.yml
│   │   └── test/
│   ├── pom.xml
│   ├── mvnw & mvnw.cmd
│   └── README.md
│
└── README.md
```

---

# RUNNING BOTH SERVICES

## Step 1: Build Both Services

```bash
# Navigate to root directory
cd FlowBoard-Backend

# Build eureka-server
cd eureka_server
mvn clean install -DskipTests
cd ..

# Build api-gateway
cd api-gateway
mvn clean install -DskipTests
cd ..
```

## Step 2: Start Eureka Server

```bash
cd eureka_server

# Using Maven
mvn spring-boot:run

# Or using Java JAR
java -jar target/eureka-server-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
... : Tomcat started on port(s): 8761 (http) with context path ''
... : Started EurekaServerApplication in X.XXX seconds
```

**Verify**: Open browser to `http://localhost:8761/`

## Step 3: Start API Gateway

In a new terminal:

```bash
cd api-gateway

# Using Maven
mvn spring-boot:run

# Or using Java JAR
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
... : Tomcat started on port(s): 8080 (http) with context path ''
... : Started ApiGatewayApplication in X.XXX seconds
```

**Verify**: 
```bash
curl http://localhost:8080/actuator/gateway/routes
```

## Step 4: Register Microservices

Each microservice needs to register with Eureka. Add to their `application.yml`:

```yaml
spring:
  application:
    name: my-service      # Unique service name

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

And add dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

## Step 5: Configure Routes in API Gateway

Update `api-gateway/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=2
        
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=2
        
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=2
```

---

# SERVICE CONFIGURATION

## How Services Work Together

1. **Eureka Server Starts**: Service registry ready at `http://localhost:8761`
2. **API Gateway Starts**: Registers itself with Eureka, connects to registry
3. **Microservices Start**: Each service registers with Eureka
4. **Request Flow**:
   - Client sends request to API Gateway (`http://localhost:8080/api/users/1`)
   - Gateway queries Eureka for `user-service` instances
   - Gateway applies predicates and filters
   - Gateway routes request to available instance
   - Service processes request and returns response
   - Gateway returns response to client

## Example: Complete Flow

```
Client Request:
  GET http://localhost:8080/api/users/1

API Gateway Processing:
  1. Matches route with predicate Path=/api/users/**
  2. Applies filter StripPrefix=2
  3. Queries Eureka: "Where is user-service?"
  4. Gets instances: [user-service:8081, user-service:8082]
  5. Load-balances to available instance
  6. Transforms request path: /users/1 → /1

Upstream Service:
  Processes GET /1
  Returns user data

API Gateway:
  Returns response to client
```

## Monitoring Services

### Check Eureka Dashboard
```
http://localhost:8761/
```

Shows all registered services and their instances with status.

### Check Registered Services via API
```bash
curl -X GET http://localhost:8761/eureka/apps \
  -H "Accept: application/json"
```

### Check API Gateway Routes
```bash
curl http://localhost:8080/actuator/gateway/routes
```

---

# ADVANCED TOPICS

## Load Balancing Strategies

Configure different load balancing strategies:

```yaml
spring:
  cloud:
    loadbalancer:
      ribbon:
        enabled: false
    gateway:
      routes:
        - id: my-service
          uri: lb://my-service
          predicates:
            - Path=/api/**
```

## Request Rate Limiting

Add RequestRateLimiter filter (requires Redis):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: limited-service
          uri: lb://my-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
```

## Request/Response Transformation

```yaml
filters:
  - name: AddRequestHeader
    args:
      name: X-Request-Id
      value: ${request.header.x-request-id}
  
  - name: RewritePath
    args:
      regexp: /api(?<segment>.*)
      replacement: ${segment}
```

## Clustering Eureka Servers

For production, cluster multiple Eureka instances:

```yaml
eureka:
  instance:
    hostname: eureka-server-1
  client:
    serviceUrl:
      defaultZone: http://eureka-server-2:8761/eureka/,http://eureka-server-3:8761/eureka/
```

## Production Security

1. **Enable HTTPS** on both services
2. **Add Authentication** to API Gateway
3. **Configure CORS** for cross-origin requests
4. **Implement Rate Limiting** per client
5. **Enable Logging & Monitoring**

### Example HTTPS Configuration

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12
```

## Troubleshooting

### Services Not Registering with Eureka

**Issue**: Microservices don't appear in Eureka dashboard

**Solutions**:
- Check if Eureka Server is running: `curl http://localhost:8761/`
- Verify service has Eureka Client dependency
- Check `eureka.client.service-url.defaultZone` is correct
- Review service startup logs for Eureka registration errors
- Ensure service name is set: `spring.application.name: my-service`

### API Gateway Can't Find Services

**Issue**: Routes to services fail with 503 Service Unavailable

**Solutions**:
- Verify service is registered in Eureka dashboard
- Check service name in route matches `spring.application.name`
- Use `lb://service-name` format for dynamic routing
- Check network connectivity between gateway and services
- Review gateway logs for route matching errors

### Eureka Server High Memory Usage

**Solutions**:
- Monitor number of registered instances
- Adjust JVM heap: `java -Xmx512m -Xms256m -jar eureka-server-0.0.1-SNAPSHOT.jar`
- Configure instance eviction policies
- Enable self-preservation mode

### Instances Evicted Unexpectedly

**Solutions**:
- Check renewal threshold configuration
- Verify network connectivity
- Increase heartbeat interval in services
- Review Eureka server logs for eviction details

---

## Performance Tuning

### JVM Optimization

```bash
java -Xmx1g -Xms512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar api-gateway-0.0.1-SNAPSHOT.jar
```

### Connection Pool Configuration

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
        response-timeout: 10s
```

---

## References

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Netflix Eureka Documentation](https://spring.io/projects/spring-cloud-netflix)
- [Netflix Eureka GitHub](https://github.com/Netflix/eureka)
- [Project Reactor Documentation](https://projectreactor.io/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

## Support & Contribution

For issues, questions, or contributions, please refer to the main [FlowBoard-Backend](../README.md) repository.

## License

This project is part of the FlowBoard Backend and follows the same license terms.
