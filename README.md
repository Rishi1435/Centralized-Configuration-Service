# Centralized, Git-Backed Configuration Service

A robust microservices configuration management system built with **Spring Cloud Config Server** and a Spring Boot config client (**inventory-service**). Configurations are stored in a local Git repository (`config-repo`) and dynamically loaded by the client. It features environment-specific properties (for `dev` and `prod` profiles), zero-downtime configuration updates using Spring Boot Actuator's `@RefreshScope`, health monitoring, and orchestration using Docker Compose.

---

## 📺 Video Demo

Watch the full walkthrough and live demonstration of the centralized configuration service in action:

[![Centralized Config Service Demo](https://img.shields.io/badge/YouTube-Watch%20Demo-red?style=for-the-badge&logo=youtube&logoColor=white)](https://youtu.be/SRCrdJq6MOc)

---

## Architecture Overview

```
                      +-------------------+
                      |    Local Git      |
                      |   config-repo/    |
                      +---------+---------+
                                |
                                | (Reads files via git protocol/file)
                                v
                      +---------+---------+
                      |   config-server   | (Port 8888)
                      +---------+---------+
                                |
                                | (Fetches configuration on startup/refresh)
                                v
                      +---------+---------+
                      | inventory-service | (Port 8081 for dev / 8082 for prod)
                      +-------------------+
```

1. **Git Repository (`config-repo`)**: Serves as the single source of truth containing environment-specific YAML files (`inventory-service-dev.yml` and `inventory-service-prod.yml`).
2. **Spring Cloud Config Server (`config-server`)**: Connects to the local Git repository and exposes endpoints to serve configuration to client services.
3. **Inventory Service (`inventory-service`)**: Connects to the Config Server during the bootstrap phase, fetches its properties, binds them to Spring components, and exposes APIs with dynamic refresh support.

---

## Features

- **Environment Isolation**: Separate configurations for `dev` (port `8081`, maxStock `100`) and `prod` (port `8082`, maxStock `10000`) profiles.
- **Fail-Fast Startup**: The client fails to start if the config server is unavailable (`spring.cloud.config.fail-fast=true`), ensuring services do not run with bad configurations.
- **Dynamic Configuration Refresh**: Modify configuration values in the Git repository, commit them, and notify the client using the POST `/actuator/refresh` endpoint to hot-reload properties without restarting the service.
- **Custom Health Indicator**: Custom health endpoint `/api/inventory/health` that dynamically checks and verifies connection status with the Config Server.

---

## Setup & Running the Services

### Prerequisites
- Docker and Docker Compose installed on your host machine.
- Git installed on your host machine.

### Run with Docker Compose
To build and start both the config-server and the inventory-service in one command:

```bash
docker-compose up --build
```

- **Config Server** will start and listen on port `8888`.
- **Inventory Service** will wait for the Config Server to become healthy and then start on port `8081` (active profile `dev`).

To stop the containers:
```bash
docker-compose down
```

---

## Environment Variables

An `.env.example` file is included in the project root. To customize environment variables, copy it to a `.env` file:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | The active Spring profile for the inventory-service (`dev` or `prod`) | `dev` |

---

## API & Verification Endpoints

### 1. Config Server Configurations
Retrieve configuration properties served by the Config Server for the `dev` profile:
- **Endpoint**: `GET http://localhost:8888/inventory-service/dev`
- **Response**:
```json
{
  "name": "inventory-service",
  "profiles": [
    "dev"
  ],
  "label": null,
  "version": "9011b22e1b12b...",
  "propertySources": [
    {
      "name": "file:///etc/config-repo/inventory-service-dev.yml",
      "source": {
        "inventory.maxStock": 100,
        "inventory.replenishThreshold": 10,
        "server.port": 8081
      }
    }
  ]
}
```

### 2. Client Active Configurations
Retrieve the configurations active in the running `inventory-service`:
- **Endpoint**: `GET http://localhost:8081/api/inventory/config`
- **Response**:
```json
{
  "profile": "dev",
  "maxStock": 100,
  "replenishThreshold": 10
}
```

### 3. Custom Connection Health Check
Check the connection health status from `inventory-service` to the Config Server:
- **Endpoint**: `GET http://localhost:8081/api/inventory/health`
- **Response**:
```json
{
  "status": "UP",
  "configServer": "connected"
}
```

---

## Dynamic Refresh Workflow (Verification Steps)

To update configuration properties dynamically without restarting the `inventory-service`:

1. **Verify current config values**:
   ```bash
   curl http://localhost:8081/api/inventory/config
   # Output: {"profile":"dev","maxStock":100,"replenishThreshold":10}
   ```

2. **Update the config repository**:
   Open `config-repo/inventory-service-dev.yml` on the host and change `inventory.maxStock` to `250`:
   ```yaml
   inventory:
     maxStock: 250
     replenishThreshold: 10
   server:
     port: 8081
   ```

3. **Commit the changes to Git**:
   Inside the `config-repo` directory, run:
   ```bash
   git add .
   git commit -m "Update maxStock to 250"
   ```

4. **Trigger refresh on the client**:
   Send an empty `POST` request to the client's refresh Actuator endpoint:
   ```bash
   curl -X POST http://localhost:8081/actuator/refresh
   # Output: ["inventory.maxStock"]
   ```

5. **Verify updated config values**:
   Query the configuration endpoint again:
   ```bash
   curl http://localhost:8081/api/inventory/config
   # Output: {"profile":"dev","maxStock":250,"replenishThreshold":10}
   ```
