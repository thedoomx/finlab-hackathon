# Cache Configuration

## Overview

The FinLab Validator uses Redis for caching with two primary cache types:

1. **JWT Tokens** - Authentication session tokens
2. **IBAN Lookups** - Account validation results

## Cache Settings

### JWT Token Cache
- **TTL**: 1 hour (configurable via `REDIS_AUTH_TTL`)
- **Storage**: Redis
- **Purpose**: Stateful JWT authentication, session management

### IBAN Lookup Cache
- **TTL**: 60 minutes for production (configurable via `REDIS_IBAN_TTL`, default 10 minutes)
- **Max Entries**: 1,000,000 for production (configurable via `IBAN_CACHE_MAX_ENTRIES`, default 100,000)
- **Storage**: Redis
- **Purpose**: Performance optimization for frequently validated IBANs

## Cache Warmup

### Automatic Warmup on Access
The IBAN cache is populated automatically on first access. When an IBAN is validated:
1. Check Redis cache
2. If miss, query PostgreSQL database
3. Store result in Redis with 10-minute TTL
4. Return result

### Database Pre-population
The database is pre-populated with 1,000,000 valid Bulgarian IBANs during initial Docker startup:
- SQL migration script: `V3__seed_ibans_with_random_status.sql`
- IBAN format: `BG` + 2 check digits + `BANK` + 14 random digits
- Status distribution: Random (ALLOW, REVIEW, BLOCK)
- Generation time: ~2-5 minutes on first startup

## Configuration

Environment variables in `infra/.env`:

```properties
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_IBAN_TTL=60m
REDIS_AUTH_TTL=1h
IBAN_CACHE_MAX_ENTRIES=1000000
```

**Note**: Production values (docker-compose) are optimized for performance:
- IBAN cache TTL: 60 minutes (default 10 minutes for dev)
- Max entries: 1,000,000 (default 100,000 for dev)
- Matches the database seed size of 1M IBANs

## Cache Behavior

### Cache Hit Flow
```
Client → Gateway → Validator → Redis (HIT) → Return cached result
```

### Cache Miss Flow
```
Client → Gateway → Validator → Redis (MISS) → PostgreSQL → Cache result → Return
```

## Monitoring

Check cache statistics:
```bash
docker exec validator-redis redis-cli INFO stats
```

View cached keys:
```bash
docker exec validator-redis redis-cli KEYS "*"
```

Check IBAN cache entries:
```bash
docker exec validator-redis redis-cli DBSIZE
```

## Performance Impact

- **Cache Hit**: < 5ms response time
- **Cache Miss**: 50-200ms response time (includes DB query + cache update)
- **Cache Hit Ratio**: Typically 85-95% in production workloads
