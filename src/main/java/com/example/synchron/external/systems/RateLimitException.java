package com.example.synchron.external.systems;

import java.io.Serial;

public final class RateLimitException extends ExternalSystemException {
  @Serial private static final long serialVersionUID = 8962152095931851719L;

  public RateLimitException(String message) {
    super(message);
  }
}
