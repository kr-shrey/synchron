package com.example.synchron.sync;

import com.example.synchron.external.systems.*;
import com.example.synchron.rules.RuleEngine;
import com.example.synchron.rules.decision.Decision;
import com.example.synchron.rules.decision.Drop;
import com.example.synchron.rules.decision.Sync;
import com.example.synchron.schemas.InternalSystemSchema;
import com.example.synchron.schemas.SyncEvent;
import com.example.synchron.transformers.Transformer;
import com.example.synchron.transformers.TransformerRegistry;
import com.example.synchron.utils.RedisLikeStore;
import com.example.synchron.utils.TokenBucketRateLimiter;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class SyncWorker {

  public static final int MAX_ATTEMPTS = 3;
  public static final long BASE_BACKOFF_MS = 1000L;
  public static final int IDEMPOTENCY_LOCK_TTL_SECONDS = 60;
  public static final long RATE_LIMITER_TIMEOUT_MS = 10000L;
  public static final String OK_RESULT = "ok";
  private final TransformerRegistry transformers;
  private final ExternalSystemClientFactory externalSystemClientFactory;
  private final RuleEngine ruleEngine;
  private final TokenBucketRateLimiter rateLimiter;
  private final RedisLikeStore idempotency;
  private final IdMappingStore idMapping;
  private final DeadLetterQueue dlq;
  private final Random rng = new Random();

  public SyncWorker() {
    this.externalSystemClientFactory = ExternalSystemClientFactory.getInstance();
    this.transformers = TransformerRegistry.getInstance();
    this.ruleEngine = RuleEngine.getInstance();
    this.rateLimiter = TokenBucketRateLimiter.getInstance();
    this.idempotency = RedisLikeStore.getInstance();
    this.idMapping = IdMappingStore.getInstance();
    this.dlq = DeadLetterQueue.getInstance();
  }

  public SyncResult process(SyncEvent event) {

    // evaluate rules
    SyncEvent processedEvent = event;
    Decision decision =
        ruleEngine.evaluate(
            event.tenantId(), event.externalSystemName(), event.operation(), event.record());

    if (decision instanceof Drop(String reason)) {
      log.debug("Skipping event {}: {}", event.eventId(), reason);
      return SyncResult.skip(event.eventId(), reason);
    } else if (decision instanceof Sync(InternalSystemSchema filteredRecord)) {
      processedEvent = event.withRecord(filteredRecord);
    }

    // check processed events
    RedisLikeStore.State state =
        idempotency.reserve(processedEvent.deduplicationKey(), IDEMPOTENCY_LOCK_TTL_SECONDS);

    if (state == RedisLikeStore.State.COMPLETED) {
      return SyncResult.skip(processedEvent.eventId(), "already completed (idempotent)");
    } else if (state == RedisLikeStore.State.IN_PROGRESS) {
      return SyncResult.skip(processedEvent.eventId(), "another worker in progress");
    }

    // process with retry
    int attempts = 0;
    String lastError = "";
    while (attempts < MAX_ATTEMPTS) {
      attempts++;
      try {
        rateLimiter.acquire(
            TokenBucketRateLimiter.getBucketKey(
                processedEvent.tenantId(), processedEvent.externalSystemName()),
            RATE_LIMITER_TIMEOUT_MS);
        dispatch(processedEvent);
        idempotency.complete(
            processedEvent.deduplicationKey(), OK_RESULT, IDEMPOTENCY_LOCK_TTL_SECONDS);
        return SyncResult.ok(processedEvent.eventId(), attempts);

      } catch (ExternalSystemException e) {
        ClassifiedError classified = null;
        if (e instanceof PermanentException) {
          classified = new ClassifiedError("permanent: " + e.getMessage(), false);
        } else if (e instanceof TransientException) {
          classified = new ClassifiedError("transient: " + e.getMessage(), true);
        } else if (e instanceof RateLimitException) {
          classified = new ClassifiedError("rate_limit: " + e.getMessage(), true);
        } else {
          classified = new ClassifiedError("unknown: " + e.getMessage(), false);
        }

        lastError = classified.message;
        if (!classified.retryable) {
          log.warn("Permanent error for event {}: {}", processedEvent.eventId(), e.getMessage());
          break;
        }
        if (attempts >= MAX_ATTEMPTS) break;
        backoff(attempts);

      } catch (TokenBucketRateLimiter.RateLimitTimeoutException e) {
        lastError = "rate_limit_timeout: " + e.getMessage();
        if (attempts >= MAX_ATTEMPTS) break;
        backoff(attempts);

      } catch (IllegalArgumentException e) {
        lastError = "validation: " + e.getMessage();
        log.warn(
            "Validation failed, error for event {}: {}", processedEvent.eventId(), e.getMessage());
        break;

      } catch (RuntimeException e) {
        lastError = "unexpected: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        log.error("Unexpected error for event {}", processedEvent.eventId(), e);
        break;
      }
    }

    idempotency.release(processedEvent.deduplicationKey());
    dlq.add(processedEvent, lastError, attempts);
    return SyncResult.fail(processedEvent.eventId(), attempts, lastError);
  }

  private void dispatch(SyncEvent event) {
    ExternalSystemClient externalSystem =
        externalSystemClientFactory.get(event.externalSystemName());

    Transformer transformer = transformers.get(event.externalSystemName());
    Map<String, Object> payload = transformer.toWireFormat(event.record());

    if (event.operation() == com.example.synchron.schemas.Operation.CREATE) {
      Map<String, Object> response = externalSystem.create(payload);
      Object externalId = response.get("external_id");
      if (externalId != null) {
        idMapping.set(
            event.tenantId(),
            event.externalSystemName(),
            event.record().internalId(),
            externalId.toString());
      }
    } else if (event.operation() == com.example.synchron.schemas.Operation.UPDATE) {
      Optional<String> externalIdOpt =
          idMapping.get(event.tenantId(), event.externalSystemName(), event.record().internalId());
      if (externalIdOpt.isEmpty()) {
        log.error(
            "Update event failed, external_id not known for record id: {}; push to DLQ",
            event.record().internalId());
        throw new RuntimeException(
            "Update event failed, external_id not known for record id: "
                + event.record().internalId());
      } else {
        externalSystem.update(externalIdOpt.get(), payload);
      }
    } else if (event.operation() == com.example.synchron.schemas.Operation.DELETE) {
      Optional<String> externalIdOpt =
          idMapping.get(event.tenantId(), event.externalSystemName(), event.record().internalId());
      if (externalIdOpt.isEmpty()) {
        log.error(
            "Delete event failed, external_id not known for record id: {}; push to DLQ",
            event.record().internalId());
        throw new RuntimeException(
            "Delete event failed, external_id not known for record id: "
                + event.record().internalId());
      }
      externalSystem.delete(externalIdOpt.get());
      idMapping.delete(event.tenantId(), event.externalSystemName(), event.record().internalId());
    }
  }

  private void backoff(int attempt) {
    long sleepMs = BASE_BACKOFF_MS * attempt;
    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private record ClassifiedError(String message, boolean retryable) {}
}
