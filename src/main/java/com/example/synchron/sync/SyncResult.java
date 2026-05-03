package com.example.synchron.sync;

public record SyncResult(
    String eventId,
    boolean success,
    boolean skipped,
    String skipReason,
    int attempts,
    String error) {
  public static SyncResult ok(String eventId, int attempts) {
    return new SyncResult(eventId, true, false, "", attempts, "");
  }

  public static SyncResult skip(String eventId, String reason) {
    return new SyncResult(eventId, true, true, reason, 0, "");
  }

  public static SyncResult fail(String eventId, int attempts, String error) {
    return new SyncResult(eventId, false, false, "", attempts, error);
  }
}
