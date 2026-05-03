package com.example.synchron.transformers;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public final class TransformerRegistry {

  private static TransformerRegistry instance;
  private final Map<String, Transformer> transformers = new ConcurrentHashMap<>();

  private TransformerRegistry() {}

  public static synchronized TransformerRegistry getInstance() {
    if (instance == null) {
      instance = new TransformerRegistry();
    }
    return instance;
  }

  public void register(Transformer transformer) {
    if (transformers.putIfAbsent(transformer.externalSystemName(), transformer) != null) {
      throw new IllegalStateException(
          "Transformer already registered for externalSystemName '"
              + transformer.externalSystemName()
              + "'");
    }
  }

  public Transformer get(String externalSystemName) {
    Transformer t = transformers.get(externalSystemName);
    if (t == null) {
      throw new NoSuchElementException(
          "No transformer registered for externalSystemName '"
              + externalSystemName
              + "'. Registered: "
              + transformers.keySet());
    }
    return t;
  }
}
