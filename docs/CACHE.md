# Cache Configuration

## Overview

The FinLab Validator uses Redis for caching with two primary cache types:

1. **JWT Tokens** - Authentication session tokens
2. **IBAN Lookups** - Account validation results

## Cache Settings

### JWT Token Cache
- **Token Expiration**: 1 hour (configurable via `AUTH_TTL`)
- **Redis Storage TTL**: 1 hour (configurable via `REDIS_AUTH_TTL`)
- **Storage**: Redis and Database
- **Purpose**: Stateful JWT authentication, session management

### IBAN Lookup Cache
- **TTL**: 60 minutes for production (configurable via `REDIS_IBAN_TTL`, default 10 minutes)
- **Max Entries**: 1,000,000 for production (configurable via `IBAN_CACHE_MAX_ENTRIES`, default 100,000)
- **Storage**: Redis
- **Purpose**: Performance optimization for frequently validated IBANs

## Cache Warmup

### Automatic Background Warmup (Production)
In production, the IBAN cache is automatically pre-warmed on application startup:
- **Enabled by default** via `IBAN_CACHE_WARMUP_ENABLED=true`
- **Non-blocking**: Runs in background, doesn't delay service health check
- **Loads**: Up to 1,000,000 IBANs (configurable via `IBAN_CACHE_MAX_ENTRIES`)
- **Time**: ~40 seconds for 1M entries
- **Page size**: 10,000 entries per batch
- **Logging**: Progress logged every page (1/100, 2/100, etc.)

**Startup Behavior:**
1. Service becomes healthy immediately (healthcheck passes)
2. Cache warmer runs asynchronously in background
3. Gateway and UI can start without waiting for cache warmup
4. Requests during warmup are served from database (cache miss)

**Scheduled Refresh:**
- Cache is automatically refreshed every hour (cron: `0 0 * * * *`)
- Ensures cache stays populated even as TTLs expire
- Also runs in background without blocking requests

### Automatic Warmup on Access
If cache warmup is disabled or for cache misses, the IBAN cache is populated on first access:
1. Check Redis cache
2. If miss, query PostgreSQL database
3. Store result in Redis with configured TTL
4. Return result

### Database Pre-population
The database is pre-populated with 1,000,000 valid Bulgarian IBANs during initial Docker startup:
- SQL migration script: `V3__seed_ibans_with_random_status.sql`
- IBAN format: `BG` + 2 check digits + `BANK` + 14 random digits
- Status distribution: Random (ALLOW, REVIEW, BLOCK)
- Generation time: ~1-2 minutes on first startup

## Configuration

Environment variables in `infra/.env`:

```properties
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_IBAN_TTL=60m
REDIS_AUTH_TTL=1h
AUTH_TTL=1h
IBAN_CACHE_MAX_ENTRIES=1000000
IBAN_CACHE_WARMUP_ENABLED=true
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

## Performance Impact

- **Cache Hit**: < 5ms response time
- **Cache Miss**: 50-150ms response time (includes DB query + cache update)
