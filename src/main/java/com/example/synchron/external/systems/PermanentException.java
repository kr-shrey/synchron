package com.example.synchron.external.systems;

import java.io.Serial;

public final class PermanentException extends ExternalSystemException {
  @Serial private static final long serialVersionUID = -3485878213701390687L;

  public PermanentException(String message) {
    super(message);
  }

  public PermanentException(String message, Throwable cause) {
    super(message, cause);
  }
}
