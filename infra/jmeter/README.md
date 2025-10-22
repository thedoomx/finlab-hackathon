# JMeter Load Testing for FinLab Validator

This directory contains Apache JMeter test plans for load and stress testing the FinLab Validator API Gateway.

## Test Scenarios

### 1. Normal Load Test (`normal-load.jmx`)
Simulates realistic production load to validate system performance under normal conditions.

**Configuration:**
- **Threads (Users):** 50
- **Ramp-up Period:** 30 seconds
- **Loops:** 10 iterations per user
- **Total Requests:** 500 requests
- **Target:** `/api/v1/accounts/{iban}` endpoint

**Performance Thresholds:**
- Response Time: < 500ms
- Error Rate: 0% (all requests must return HTTP 200)
- Valid Response: Must be one of `ALLOW`, `REVIEW`, or `BLOCK`

### 2. Extreme Load Test (`extreme-load.jmx`)
Stress test to identify system breaking points and performance degradation under extreme load.

**Configuration:**
- **Threads (Users):** 500
- **Ramp-up Period:** 60 seconds
- **Loops:** 50 iterations per user
- **Total Requests:** 25,000 requests
- **Target:** `/api/v1/accounts/{iban}` endpoint

**Performance Thresholds:**
- Response Time: < 1000ms
- Error Rate: < 5% acceptable under extreme load
- Valid Response: Must be one of `ALLOW`, `REVIEW`, or `BLOCK`

## Prerequisites

1. **Docker Compose running:**
   ```bash
   cd infra
   docker-compose up -d
   ```

2. **Gateway and Validator services must be running:**
   - Gateway at `https://localhost:443`
   - Validator service accessible from gateway
   - PostgreSQL database initialized with test data
   - Redis cache available

3. **Valid test IBAN:**
   - Default: `BG80BNBG96611020345678`
   - Must exist in the database with a valid status (ALLOW/REVIEW/BLOCK)

## Running Tests

### Using Docker (Recommended)

#### Normal Load Test
```bash
# Start JMeter container if not running
cd infra
docker-compose up -d jmeter

# Execute normal load test
docker exec validator-jmeter jmeter -n \
  -t /tests/normal-load.jmx \
  -l /results/normal-load-results.jtl \
  -j /results/normal-load.log \
  -Jgateway.host=host.docker.internal \
  -Jgateway.port=443

# View results
cat ../stress_tests/normal-load-results.jtl
```

#### Extreme Load Test
```bash
# Execute extreme load test
docker exec validator-jmeter jmeter -n \
  -t /tests/extreme-load.jmx \
  -l /results/extreme-load-results.jtl \
  -j /results/extreme-load.log \
  -Jgateway.host=host.docker.internal \
  -Jgateway.port=443

# View results
cat ../stress_tests/extreme-load-results.jtl
```

### Custom Parameters

You can override test parameters using JMeter properties:

```bash
docker exec validator-jmeter jmeter -n \
  -t /tests/normal-load.jmx \
  -l /results/custom-test-results.jtl \
  -Jgateway.host=mygateway.com \
  -Jgateway.port=8443
```

### Generating HTML Reports

After running tests, generate HTML dashboard reports:

```bash
# Generate report from normal load test
docker exec validator-jmeter jmeter -g /results/normal-load-results.jtl \
  -o /results/normal-load-report

# Generate report from extreme load test
docker exec validator-jmeter jmeter -g /results/extreme-load-results.jtl \
  -o /results/extreme-load-report
```

HTML reports will be available in:
- `stress_tests/normal-load-report/index.html`
- `stress_tests/extreme-load-report/index.html`

## Results Location

All test results are saved to the `/stress_tests` directory in the repository root:

```
stress_tests/
├── normal-load-results.jtl       # CSV results from normal load test
├── normal-load-detailed.jtl      # Detailed results from normal load test
├── normal-load.log                # JMeter execution log
├── normal-load-report/            # HTML dashboard (if generated)
├── extreme-load-results.jtl      # CSV results from extreme load test
├── extreme-load-detailed.jtl     # Detailed results from extreme load test
├── extreme-load.log               # JMeter execution log
└── extreme-load-report/           # HTML dashboard (if generated)
```

## Interpreting Results

### Key Metrics

1. **Average Response Time:** Should be well below the threshold (500ms normal, 1000ms extreme)
2. **90th Percentile (p90):** 90% of requests completed within this time
3. **95th Percentile (p95):** 95% of requests completed within this time
4. **Throughput:** Requests per second the system can handle
5. **Error Rate:** Percentage of failed requests (should be 0% for normal load)

### Success Criteria

**Normal Load Test:**
- ✅ Error Rate = 0%
- ✅ Average Response Time < 300ms
- ✅ p95 Response Time < 500ms
- ✅ All responses are valid IBAN statuses

**Extreme Load Test:**
- ✅ Error Rate < 5%
- ✅ Average Response Time < 800ms
- ✅ p95 Response Time < 1000ms
- ✅ System remains stable (no crashes)

## Troubleshooting

### Connection Refused Errors
- Ensure gateway is running: `curl -k https://localhost:443/actuator/health`
- Check network connectivity between JMeter container and gateway
- Use `host.docker.internal` instead of `localhost` when running in Docker

### High Error Rates
- Verify database has test IBAN data
- Check Redis is running and accessible
- Review gateway and validator logs for errors
- Ensure sufficient system resources (CPU, memory)

### SSL/TLS Errors
- JMeter may need SSL certificate configuration for HTTPS endpoints
- Consider using HTTP for testing or importing certificates into JMeter

## Cleaning Up

Remove all test results:
```bash
# On Unix/Linux/Mac
rm -rf ../stress_tests/*.jtl ../stress_tests/*.log ../stress_tests/*-report

# On Windows
del /Q ..\stress_tests\*.jtl ..\stress_tests\*.log
rmdir /S /Q ..\stress_tests\*-report
```

Stop JMeter container:
```bash
docker-compose stop jmeter
```
