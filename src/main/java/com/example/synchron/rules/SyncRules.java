package com.example.synchron.rules;

import com.example.synchron.schemas.InternalSystemSchema;
import com.example.synchron.schemas.Operation;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Example Rules
 *
 * @param allowedOperations which CRUD ops we're allowed to send to this externalSystem
 * @param outboundFieldBlocklist fields that should be cleared before sending
 * @param predicate callable that returns true if the record should sync; optional
 */
public record SyncRules(
    Set<Operation> allowedOperations,
    Set<String> outboundFieldBlocklist,
    Predicate<InternalSystemSchema> predicate) {
  public SyncRules {
    allowedOperations =
        allowedOperations == null
            ? EnumSet.allOf(Operation.class)
            : EnumSet.copyOf(allowedOperations);
    outboundFieldBlocklist =
        outboundFieldBlocklist == null ? Set.of() : Set.copyOf(outboundFieldBlocklist);
  }

  public static SyncRules permissive() {
    return new SyncRules(EnumSet.allOf(Operation.class), Set.of(), null);
  }

  public boolean operationAllowed(Operation op) {
    return allowedOperations.contains(op);
  }

  public boolean shouldSync(InternalSystemSchema record) {
    return predicate == null || predicate.test(record);
  }

  public InternalSystemSchema applyFieldFilter(InternalSystemSchema record) {
    if (outboundFieldBlocklist.isEmpty()) return record;
    return InternalSystemSchema.builder()
        .internalId(record.internalId())
        .firstName(record.firstName())
        .lastName(record.lastName())
        .email(blockOrKeep("email", record.email()))
        .phone(blockOrKeep("phone", record.phone()))
        .company(blockOrKeep("company", record.company()))
        .updatedAt(record.updatedAt())
        .sourceVersion(record.sourceVersion())
        .build();
  }

  private String blockOrKeep(String fieldName, String value) {
    return outboundFieldBlocklist.contains(fieldName) ? null : value;
  }
}
