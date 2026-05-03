package com.example.synchron.sync;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory mock for DAO to map Internal system IDs to external IDs. */
public final class IdMappingStore {

  private static IdMappingStore instance;
  private final ConcurrentHashMap<Key, String> map = new ConcurrentHashMap<>();

  private IdMappingStore() {}

  public static synchronized IdMappingStore getInstance() {
    if (instance == null) {
      instance = new IdMappingStore();
    }
    return instance;
  }

  public Optional<String> get(String tenantId, String externalSystem, String internalId) {
    return Optional.ofNullable(map.get(new Key(tenantId, externalSystem, internalId)));
  }

  public void set(String tenantId, String externalSystem, String internalId, String externalId) {
    map.put(new Key(tenantId, externalSystem, internalId), externalId);
  }

  public void delete(String tenantId, String externalSystem, String internalId) {
    map.remove(new Key(tenantId, externalSystem, internalId));
  }

  private record Key(String tenantId, String externalSystem, String internalId) {}
}
