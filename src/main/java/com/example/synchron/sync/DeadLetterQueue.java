package com.example.synchron.sync;

import com.example.synchron.schemas.SyncEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DeadLetterQueue {

  private static DeadLetterQueue instance;
  private final List<Entry> entries = Collections.synchronizedList(new ArrayList<>());

  private DeadLetterQueue() {}

  public static synchronized DeadLetterQueue getInstance() {
    if (instance == null) {
      instance = new DeadLetterQueue();
    }
    return instance;
  }

  public void add(SyncEvent event, String error, int attempts) {
    entries.add(new Entry(event, error, attempts, Instant.now()));
  }

  public List<Entry> all() {
    synchronized (entries) {
      return List.copyOf(entries);
    }
  }

  public int size() {
    return entries.size();
  }

  public record Entry(SyncEvent event, String error, int attempts, Instant failedAt) {}
}
