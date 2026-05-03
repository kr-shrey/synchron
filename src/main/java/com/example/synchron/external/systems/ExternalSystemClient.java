package com.example.synchron.external.systems;

import java.util.Map;

public interface ExternalSystemClient {

  String externalSystemName();

  Map<String, Object> create(Map<String, Object> payload);

  Map<String, Object> fetch(String internalId);

  Map<String, Object> update(String internalId, Map<String, Object> payload);

  void delete(String internalId);
}
