package com.example.synchron.external.systems;

import java.io.Serial;

public final class TransientException extends ExternalSystemException {
  @Serial private static final long serialVersionUID = 3352184962926999765L;

  public TransientException(String message) {
    super(message);
  }

  public TransientException(String message, Throwable cause) {
    super(message, cause);
  }
}
