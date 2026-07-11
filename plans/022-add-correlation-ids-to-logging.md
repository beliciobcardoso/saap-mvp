# Plan 022: Add correlation/trace IDs to logging via MDC for request tracing and audit compliance

> **Executor instructions**: Read plan fully. This is a logging infrastructure upgrade. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx/compliance
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

No correlation/trace ID in logging. When multiple requests hit the system, logs are interleaved and hard to correlate. Audit logs (RNF01) should include request context. Solution: use SLF4J MDC (Mapped Diagnostic Context) to attach a trace ID to each request and include it in all logs.

## Scope

**In scope**:
- Create a servlet filter to assign and inject trace IDs into MDC
- Update logback-spring.xml to include trace ID in log pattern
- Ensure trace ID is also stored in audit logs

**Out of scope**:
- Do NOT change log levels or handlers
- Do NOT add distributed tracing (that's a bigger project)

## Steps

### Step 1: Create TraceIdFilter

New file: `infrastructure/web/filter/TraceIdFilter.java`:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    private static final String TRACE_ID = "traceId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

**Verify**: Filter created

### Step 2: Update logback-spring.xml

Edit `src/main/resources/logback-spring.xml` and update the log pattern:

```xml
<!-- OLD -->
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>

<!-- NEW -->
<pattern>%d{ISO8601} [%thread] %-5level [%X{traceId}] %logger{36} - %msg%n</pattern>
```

`%X{traceId}` inserts the MDC value.

**Verify**: Pattern updated

### Step 3: Update audit logging to include trace ID

In `AuditService`, when logging, extract the trace ID from MDC and store it:

```java
String traceId = MDC.get("traceId");
// Store traceId in the audit log entity
```

**Verify**: Audit logs now include trace ID

### Step 4: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Existing tests should pass (MDC is thread-local, safe in tests)
- Manual verification: run the app and check logs for trace ID format

## Done criteria

- [ ] TraceIdFilter created and registered
- [ ] logback-spring.xml updated with %X{traceId} in pattern
- [ ] Audit logs include trace ID
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] Log output shows trace IDs (manual check)

## STOP conditions

Stop and report if:

- MDC causes thread-local leaks in test environment (clear on filter exit)

## Maintenance notes

**Distributed tracing**: If the app later needs distributed tracing across services, add OpenTelemetry or Sleuth (both compatible with MDC).
