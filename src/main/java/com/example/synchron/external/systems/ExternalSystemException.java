package com.example.synchron.external.systems;

import java.io.Serial;

public abstract class ExternalSystemException extends RuntimeException {
  @Serial private static final long serialVersionUID = -2582072573529780220L;

  public ExternalSystemException(String message) {
    super(message);
  }

  public ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
  }
}
