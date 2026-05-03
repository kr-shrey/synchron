package com.example.synchron.transformers;

import static com.example.synchron.Demo.EXAMPLE_EXTERNAL_SYSTEM;

import com.example.synchron.schemas.ExampleExternalSystemSchema;
import com.example.synchron.schemas.ExternalSystemSchema;
import com.example.synchron.schemas.InternalSystemSchema;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ExampleExternalSchemaTransformer implements Transformer {

  private static void putIfPresent(Map<String, Object> map, String key, Object value) {
    if (value != null) map.put(key, value);
  }

  @Override
  public String externalSystemName() {
    return EXAMPLE_EXTERNAL_SYSTEM;
  }

  @Override
  public ExternalSystemSchema toExternalSystemSchema(InternalSystemSchema record) {
    return new ExampleExternalSystemSchema(
        null,
        record.firstName(),
        record.lastName(),
        record.email(),
        record.phone(),
        record.company(),
        record.internalId());
  }

  @Override
  public Map<String, Object> toWireFormat(InternalSystemSchema record) {
    var wire = new LinkedHashMap<String, Object>();
    putIfPresent(wire, "FirstName", record.firstName());
    putIfPresent(wire, "LastName", record.lastName());
    putIfPresent(wire, "Email", record.email());
    putIfPresent(wire, "Phone", record.phone());
    putIfPresent(wire, "AccountName", record.company());
    putIfPresent(wire, "InternalId", record.internalId());
    return wire;
  }

  @Override
  public InternalSystemSchema fromExternalSystemSchema(ExternalSystemSchema payload) {
    if (payload instanceof ExampleExternalSystemSchema) {
      ExampleExternalSystemSchema exampleExternalSystemSchema =
          (ExampleExternalSystemSchema) payload;
      return InternalSystemSchema.builder()
          .internalId(exampleExternalSystemSchema.externalIdC())
          .firstName(exampleExternalSystemSchema.firstName())
          .lastName(exampleExternalSystemSchema.lastName())
          .email(exampleExternalSystemSchema.email())
          .phone(exampleExternalSystemSchema.phone())
          .company(exampleExternalSystemSchema.accountName())
          .build();
    }
    throw new IllegalArgumentException(
        this.getClass().getSimpleName() + " cannot consume " + payload.getClass().getSimpleName());
  }
}
