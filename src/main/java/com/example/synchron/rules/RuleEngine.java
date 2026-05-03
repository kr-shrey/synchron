package com.example.synchron.rules;

import com.example.synchron.rules.decision.Decision;
import com.example.synchron.rules.decision.Drop;
import com.example.synchron.rules.decision.Sync;
import com.example.synchron.schemas.InternalSystemSchema;
import com.example.synchron.schemas.Operation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class RuleEngine {

  private static RuleEngine instance;
  private final Map<Key, SyncRules> rules = new ConcurrentHashMap<>();

  private RuleEngine() {}

  public static synchronized RuleEngine getInstance() {
    if (instance == null) {
      instance = new RuleEngine();
    }
    return instance;
  }

  public void register(String tenantId, String externalSystem, SyncRules syncRules) {
    rules.put(new Key(tenantId, externalSystem), syncRules);
  }

  public SyncRules getRules(String tenantId, String externalSystem) {
    return rules.getOrDefault(new Key(tenantId, externalSystem), SyncRules.permissive());
  }

  public Decision evaluate(
      String tenantId, String externalSystem, Operation operation, InternalSystemSchema record) {
    SyncRules r = getRules(tenantId, externalSystem);

    if (!r.operationAllowed(operation)) {
      log.debug(
          "Record {} dropped, operation {} is not not allowed for {}",
          record,
          operation.name().toLowerCase(),
          externalSystem);
      return new Drop(
          "operation " + operation.name().toLowerCase() + " not allowed for " + externalSystem);
    }

    if (!r.shouldSync(record)) {
      log.debug("Record {} dropped, record predicate returned false", record);
      return new Drop("record predicate returned false");
    }

    return new Sync(r.applyFieldFilter(record));
  }

  private record Key(String tenantId, String externalSystem) {}
}
