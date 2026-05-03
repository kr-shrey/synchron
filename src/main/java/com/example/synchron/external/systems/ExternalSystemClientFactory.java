package com.example.synchron.external.systems;

import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ExternalSystemClientFactory {

  private static ExternalSystemClientFactory instance;
  private final ConcurrentHashMap<String, ExternalSystemClient> externalSystemClients =
      new ConcurrentHashMap<>();

  private ExternalSystemClientFactory() {}

  public static synchronized ExternalSystemClientFactory getInstance() {
    if (instance == null) {
      instance = new ExternalSystemClientFactory();
    }
    return instance;
  }

  public void register(String name, ExternalSystemClient client) {
    if (!externalSystemClients.containsKey(name)) {
      externalSystemClients.put(name, client);
    } else {
      log.error("External System already registered:{}", name);
      throw new IllegalStateException("External System already registered:" + name);
    }
  }

  public ExternalSystemClient get(String name) {
    if (externalSystemClients.containsKey(name)) {
      return externalSystemClients.get(name);
    } else {
      log.error("{} : External System not registered", name);
      throw new IllegalStateException(name + ": External System not registered");
    }
  }
}
