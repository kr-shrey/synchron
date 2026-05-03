package com.example.synchron.utils;

import java.io.Serial;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** This uses in-memory counter; Redis to be used in final implementation */
public final class TokenBucketRateLimiter {

  public static final Integer DEFAULT_CAPACITY = 25;
  public static final Integer DEFAULT_REFILL_PER_SECOND = 10;
  private static TokenBucketRateLimiter instance;
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  private TokenBucketRateLimiter() {}

  public static synchronized TokenBucketRateLimiter getInstance() {
    if (instance == null) {
      instance = new TokenBucketRateLimiter();
    }
    return instance;
  }

  public static String getBucketKey(String tenant, String externalSystem) {
    return tenant + ":" + externalSystem;
  }

  public void configureBucket(String bucketName, int capacity, int refillPerSec) {
    buckets.computeIfAbsent(bucketName, k -> new Bucket(capacity, refillPerSec));
  }

  public void acquire(String bucketName, long timeoutMs) {
    Bucket bucket = buckets.get(bucketName);
    if (bucket == null) {
      throw new IllegalStateException("No bucket named '" + bucketName + "'");
    }
    long deadlineNanos = System.nanoTime() + timeoutMs * 1000000;

    while (true) {
      long sleepNanos;
      bucket.lock.lock();
      try {
        long now = System.nanoTime();
        bucket.refill(now);
        if (bucket.tokens >= 1.0) {
          bucket.tokens -= 1.0;
          return;
        }
        double deficit = 1.0 - bucket.tokens;
        sleepNanos = (long) ((deficit / bucket.refillPerSec) * 1000000000);
      } finally {
        bucket.lock.unlock();
      }

      long remainingNanos = deadlineNanos - System.nanoTime();
      if (remainingNanos <= 0) {
        throw new RateLimitTimeoutException("Could not acquire rate-limit token for " + bucketName);
      }
      long actualSleep = Math.min(sleepNanos, remainingNanos);
      try {
        Thread.sleep(actualSleep / 1000000, (int) (actualSleep % 1000000));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RateLimitTimeoutException("Interrupted while waiting for token");
      }
    }
  }

  private static final class Bucket {
    final double capacity;
    final double refillPerSec;
    final ReentrantLock lock = new ReentrantLock();
    double tokens;
    long lastRefillNanos;

    Bucket(double capacity, double refillPerSec) {
      this.capacity = capacity;
      this.refillPerSec = refillPerSec;
      this.tokens = capacity;
      this.lastRefillNanos = System.nanoTime();
    }

    void refill(long nowNanos) {
      double elapsedSec = (nowNanos - lastRefillNanos) / 1000000000.0;
      if (elapsedSec > 0) {
        tokens = Math.min(capacity, tokens + elapsedSec * refillPerSec);
        lastRefillNanos = nowNanos;
      }
    }
  }

  public static final class RateLimitTimeoutException extends RuntimeException {
    @Serial private static final long serialVersionUID = 5133140117203857899L;

    public RateLimitTimeoutException(String msg) {
      super(msg);
    }
  }
}
