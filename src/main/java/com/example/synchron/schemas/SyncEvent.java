package com.example.synchron.schemas;

import java.time.Instant;
import java.util.Objects;

public record SyncEvent(
    String eventId,
    String tenantId,
    String externalSystemName,
    Operation operation,
    InternalSystemSchema record,
    Instant enqueuedAt) {
  public SyncEvent {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(externalSystemName, "externalSystemName");
    Objects.requireNonNull(operation, "operation");
    Objects.requireNonNull(record, "record");
    if (enqueuedAt == null) {
      enqueuedAt = Instant.now();
    }
  }

  public String deduplicationKey() {
    return String.join(
        ":",
        tenantId,
        externalSystemName,
        record.internalId(),
        operation.name().toLowerCase(),
        Long.toString(record.sourceVersion()));
  }

  public SyncEvent withRecord(InternalSystemSchema newRecord) {
    return new SyncEvent(eventId, tenantId, externalSystemName, operation, newRecord, enqueuedAt);
  }
}
