package com.redis.test.fi;

/** Failure talking to the fault injector, or a fault-injector operation that did not succeed. */
public class FaultInjectorException extends RuntimeException {

  public FaultInjectorException(String message) {
    super(message);
  }

  public FaultInjectorException(String message, Throwable cause) {
    super(message, cause);
  }
}
