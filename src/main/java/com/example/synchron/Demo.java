package com.example.synchron;

import com.example.synchron.external.systems.ExternalSystemClientFactory;
import com.example.synchron.external.systems.MockExternalSystemClient;
import com.example.synchron.rules.RuleEngine;
import com.example.synchron.rules.SyncRules;
import com.example.synchron.schemas.InternalSystemSchema;
import com.example.synchron.schemas.Operation;
import com.example.synchron.schemas.SyncEvent;
import com.example.synchron.sync.DeadLetterQueue;
import com.example.synchron.sync.SyncWorker;
import com.example.synchron.transformers.ExampleExternalSchemaTransformer;
import com.example.synchron.transformers.TransformerRegistry;
import com.example.synchron.utils.TokenBucketRateLimiter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Demo {

  public static final String EXAMPLE_EXTERNAL_SYSTEM = "example_external_system";
  public static final String TENANT = "tenant_shrey";

  private Demo() {}

  public static void main(String[] args) throws Exception {
    TransformerRegistry.getInstance().register(new ExampleExternalSchemaTransformer());
    RuleEngine.getInstance().register(TENANT, EXAMPLE_EXTERNAL_SYSTEM, SyncRules.permissive());
    MockExternalSystemClient externalSystemClient =
        new MockExternalSystemClient(EXAMPLE_EXTERNAL_SYSTEM);
    ExternalSystemClientFactory.getInstance()
        .register(EXAMPLE_EXTERNAL_SYSTEM, externalSystemClient);
    TokenBucketRateLimiter.getInstance()
        .configureBucket(
            TokenBucketRateLimiter.getBucketKey(TENANT, EXAMPLE_EXTERNAL_SYSTEM),
            TokenBucketRateLimiter.DEFAULT_CAPACITY,
            TokenBucketRateLimiter.DEFAULT_REFILL_PER_SECOND);
    var worker = new SyncWorker();

    List<SyncEvent> events = generateEvents(200);
    System.out.println();
    System.out.println("Generated " + events.size() + " events");
    System.out.println();

    try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
      List<Future<?>> futures = new ArrayList<>();
      for (SyncEvent event : events) {
        futures.add(
            executor.submit(
                () -> {
                  worker.process(event);
                }));
      }
      for (Future<?> f : futures) {
        f.get();
      }
    }
    System.out.println("Total records in External Service: " + externalSystemClient.totalRecords());
    System.out.println("Total records in DLQ: " + DeadLetterQueue.getInstance().size());
    System.out.println();
  }

  private static List<SyncEvent> generateEvents(int numEvents) {
    Random rng = new Random(7);
    List<SyncEvent> events = new ArrayList<>(numEvents);
    for (int i = 0; i < numEvents; i++) {
      Operation op = null;
      if (i < numEvents / 2) {
        op = Operation.CREATE;
      } else {
        int randomInt = rng.nextInt(10);
        if (randomInt < 5) {
          op = Operation.CREATE;
        } else if (randomInt < 8) {
          op = Operation.UPDATE;
        } else {
          op = Operation.DELETE;
        }
      }
      String internalId;
      long version;
      if (op == Operation.CREATE) {
        internalId = "contact_" + i;
        version = 1;
      } else {
        internalId = "contact_" + rng.nextInt(numEvents / 2);
        version = 2 + rng.nextInt(4);
      }

      InternalSystemSchema contact =
          InternalSystemSchema.builder()
              .internalId(internalId)
              .firstName("User" + i)
              .lastName("Smith")
              .email("user" + i + "@example.com")
              .phone("+1-555-0100")
              .company("Matrix")
              .sourceVersion(version)
              .build();

      events.add(
          new SyncEvent(
              UUID.randomUUID().toString(), TENANT, EXAMPLE_EXTERNAL_SYSTEM, op, contact, null));
    }
    return events;
  }
}
