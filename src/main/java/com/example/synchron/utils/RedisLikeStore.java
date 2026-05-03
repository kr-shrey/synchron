package com.example.synchron.utils;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RedisLikeStore {

  private static RedisLikeStore instance;
  private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

  private RedisLikeStore() {}

  public static synchronized RedisLikeStore getInstance() {
    if (instance == null) {
      instance = new RedisLikeStore();
    }
    return instance;
  }

  public State reserve(String key, long ttlSeconds) {
    long ttlNanos = ttlSeconds * 1000000L;
    long now = System.nanoTime();
    var observed = new State[] {null};
    entries.compute(
        key,
        (k, current) -> {
          if (current != null && current.expiresAtNanos > now) {
            observed[0] = current.state;
            return current;
          }
          observed[0] = State.NEW;
          return new Entry(State.IN_PROGRESS, null, now + ttlNanos);
        });
    return observed[0];
  }

  public void complete(String key, Object result, long ttlSeconds) {
    long ttlNanos = ttlSeconds * 1000000L;
    long now = System.nanoTime();
    entries.put(key, new Entry(State.COMPLETED, result, now + ttlNanos));
  }

  public void release(String key) {
    entries.compute(
        key,
        (k, current) -> {
          if (current != null && current.state == State.IN_PROGRESS) {
            return null;
          }
          return current;
        });
  }

  public Optional<Object> getResult(String key) {
    Entry e = entries.get(key);
    if (e != null && e.state == State.COMPLETED) {
      return Optional.ofNullable(e.result);
    }
    return Optional.empty();
  }

  public enum State {
    NEW,
    IN_PROGRESS,
    COMPLETED
  }

  private record Entry(State state, Object result, long expiresAtNanos) {}
}
