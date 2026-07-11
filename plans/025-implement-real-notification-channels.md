# Plan DIRECTION-002: Implement real notification channels (WhatsApp, SMS, Email) instead of console-only

> **Executor instructions**: Read plan fully. This is a direction (feature) item. Do NOT push or commit.

## Status

- **Priority**: P2 (feature gap)
- **Effort**: L (large: requires external service integration)
- **Risk**: MEDIUM (adds external dependencies, service configuration)
- **Depends on**: none
- **Category**: direction/feature
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

**Evidence**:
- `infrastructure/messaging/ConsoleNotificationService.java` is the only implementation
- Method `sendNotification()` just logs to console: `logger.info(...)`
- No async/retry/queue mechanism
- PRD (RNF03) specifies: "Notificações devem ser enviadas via WhatsApp, SMS ou Email"
- Current implementation violates the requirement

Currently, all notifications (appointment confirmations, waitlist offers, follow-up reminders) are logged to console, not sent to patients. Feature is completely missing.

## Current state

- **Implemented**: `NotificationService` interface, `ConsoleNotificationService` (logs only)
- **Missing**: Real channels (WhatsApp, SMS, Email), retries, error handling, configuration
- **Effect**: Notifications don't reach patients; compliance gap (RNF03)

## Scope

**In scope**:
- Create `EmailNotificationService` (send via SMTP or service like SendGrid)
- Create `WhatsAppNotificationService` (via Twilio or similar)
- Create `SmsNotificationService` (via SMS gateway)
- Implement retry logic and error handling
- Add configuration for service selection (can be env var)
- Add `@Async` support for non-blocking sends

**Out of scope**:
- Do NOT change the domain NotificationService interface (keep it simple)
- Do NOT add notification templates (use basic format for MVP)
- Do NOT add a notification UI/dashboard (separate feature)

## Steps

### Step 1: Choose external service providers

Decision needed:
- Email: SendGrid, AWS SES, or SMTP relay?
- WhatsApp: Twilio, AWS SNS, or other?
- SMS: Twilio, AWS SNS, or other?

For MVP: recommend Twilio (covers WhatsApp + SMS, single integration).

### Step 2: Add dependencies to pom.xml

Example for Twilio:
```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>8.x</version>
</dependency>
```

For Email, add Spring Mail:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

### Step 3: Create implementations

Three new service classes:
- `EmailNotificationService` (implements NotificationService)
- `WhatsAppNotificationService` (implements NotificationService)
- `SmsNotificationService` (implements NotificationService)

Each has:
- Constructor-injected client (Twilio, SendGrid, etc.)
- `sendNotification(phone/email, message)` implementation
- Retry logic (on transient failures)
- Logging of success/failure

### Step 4: Add configuration

```yaml
app:
  notifications:
    enabled-channels: "${NOTIFICATION_CHANNELS:console}"  # console, email, whatsapp, sms
    twilio:
      account-sid: "${TWILIO_ACCOUNT_SID}"
      auth-token: "${TWILIO_AUTH_TOKEN}"
      from-number: "${TWILIO_FROM_NUMBER}"
    email:
      from: "${MAIL_FROM:noreply@saap.local}"
```

Add property beans to select which implementation is active.

### Step 5: Make NotificationService call async

Add `@Async` to notification sends to prevent blocking the request.

## Test plan

- Mock external services in tests (don't call Twilio/SendGrid in CI)
- Integration test: verify each channel successfully sends (with mocked client)
- End-to-end test: trigger a notification (e.g., appointment confirmation) and verify it was sent

## Done criteria

- [ ] Email, WhatsApp, SMS implementations created
- [ ] Configuration added to application.yaml
- [ ] External service credentials configurable via env vars
- [ ] Retry logic implemented
- [ ] `@Async` on notification sends
- [ ] Integration tests mock external services
- [ ] `./mvnw clean test` exits 0

## STOP conditions

Stop and report if:

- External service API changes mid-implementation (choose stable provider)
- Patient phone/email fields don't exist on User/Patient models (need schema change)

## Maintenance notes

**Configuration in production**: Set NOTIFICATION_CHANNELS=email,whatsapp and provide Twilio/SendGrid credentials.

**Fallback**: Keep ConsoleNotificationService as a fallback for dev/test.

**Monitoring**: Log all sent notifications (success/failure) for audit trail (RNF01).

**Future**: Add notification templates, delivery status tracking, user preferences (SMS vs Email).
