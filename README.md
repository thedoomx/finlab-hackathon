# FinLab Validator

A microservices-based IBAN validation system with JWT authentication, Redis caching, and JMeter stress testing capabilities.

## Architecture

- **Frontend**: Angular with Nginx
- **API Gateway**: Spring Cloud Gateway
- **Validator Service**: Spring Boot microservice
- **Database**: PostgreSQL
- **Cache**: Redis
- **Testing**: JMeter

## Prerequisites

- Docker
- Docker Compose

## How to Run

Navigate to the `infra` directory and start all services:

```bash
cd infra
docker compose up --build -d
```

The application will be available at:
- Frontend: https://localhost
- API Gateway: https://localhost:8081

## Default Login

Use any username to login (e.g., `testuser`, `jmeter-user`, etc.)

## Stress Testing

Two JMeter stress test scenarios are available: normal load and extreme load.

### Prerequisites for Stress Tests

You need a valid IBAN from the database. To get one, query the database or use this example: `BG88BANK37169467302950`

### Running Normal Load Test

50 threads, 10 loops each (500 total requests):

```bash
docker exec validator-jmeter sh -c "jmeter -n -t /tests/normal-load.jmx \
  -Jgateway.host=finlab-client \
  -Jgateway.port=443 \
  -Japi.key=finlab-validator-api-key \
  -Jauth.username=jmeter-user \
  -Jtest.iban=BG88BANK37169467302950 \
  -l /results/normal-load-\$(date +%Y%m%d-%H%M%S)-results.jtl \
  -j /results/normal-load-\$(date +%Y%m%d-%H%M%S).log"
```

### Running Extreme Load Test

500 threads, 50 loops each (25,000 total requests):

```bash
docker exec validator-jmeter sh -c "jmeter -n -t /tests/extreme-load.jmx \
  -Jgateway.host=finlab-client \
  -Jgateway.port=443 \
  -Japi.key=finlab-validator-api-key \
  -Jauth.username=jmeter-user \
  -Jtest.iban=BG88BANK37169467302950 \
  -l /results/extreme-load-\$(date +%Y%m%d-%H%M%S)-results.jtl \
  -j /results/extreme-load-\$(date +%Y%m%d-%H%M%S).log"
```

### Viewing Test Results

After running the tests, view the results in the web UI:
1. Login at https://localhost
2. Navigate to "Test Results" page
3. Results are displayed with timestamps and detailed metrics

## Features

- Stateful JWT authentication with Redis
- IBAN validation using JDBC/JdbcTemplate
- 1M valid Bulgarian IBANs pre-generated in database
- Real-time stress test result visualization
- Response time metrics (average, min, max, P90, P95)
- Error rate tracking
