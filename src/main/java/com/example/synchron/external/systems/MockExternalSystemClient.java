package com.example.synchron.external.systems;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class MockExternalSystemClient implements ExternalSystemClient {

  private final String externalSystemName;
  private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1);

  public MockExternalSystemClient(String externalSystemName) {
    this.externalSystemName = externalSystemName;
  }

  @Override
  public String externalSystemName() {
    return externalSystemName;
  }

  @Override
  public Map<String, Object> create(Map<String, Object> payload) {
    String internalId = externalSystemName + "_id_" + nextId.getAndIncrement();
    var record = new HashMap<>(payload);
    record.put("internal_Id", internalId);
    store.put(internalId, record);
    log.debug("Created record {} with id: {}", record, internalId);
    return record;
  }

  @Override
  public Map<String, Object> fetch(String internalId) {
    var existing = store.get(internalId);
    if (existing == null) {
      log.error("Record with id: {} not found", internalId);
      throw new PermanentException("Record " + internalId + " not found");
    }
    return new HashMap<>(existing);
  }

  @Override
  public Map<String, Object> update(String internalId, Map<String, Object> payload) {
    var existing = store.get(internalId);
    if (existing == null) {
      log.error("Record with id: {} not found", internalId);
      throw new PermanentException("Record " + internalId + " not found");
    }
    existing.putAll(payload);
    return new HashMap<>(existing);
  }

  @Override
  public void delete(String internalId) {
    var payload = Map.<String, Object>of("internal_Id", internalId);
    store.remove(internalId);
    log.debug("Deleted record with id: {}", internalId);
  }

  public int totalRecords() {
    return store.size();
  }

  public record CallRecord(String operation, Map<String, Object> payload, boolean succeeded) {}
}
