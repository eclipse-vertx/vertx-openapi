package io.vertx.openapi;

import static io.vertx.openapi.ErrorType.INVALID_SPEC;
import static io.vertx.openapi.ErrorType.UNSUPPORTED_SPEC;

public class RouterBuilderException extends RuntimeException {

  private final ErrorType type;

  public RouterBuilderException(String message, ErrorType type, Throwable cause) {
    super(message, cause);
    this.type = type;
  }

  public ErrorType type() {
    return type;
  }

  public static RouterBuilderException createInvalidContract(String reason) {
    return createInvalidContract(reason, null);
  }

  public static RouterBuilderException createInvalidContract(String reason, Throwable cause) {
    String msg = "The passed OpenAPI contract is invalid" + reason == null ? "." : ": ";
    return new RouterBuilderException(msg, INVALID_SPEC, cause);
  }

  public static RouterBuilderException createUnsupportedVersion(String version) {
    return new RouterBuilderException("The version of the passed OpenAPI contract is not supported: " + version, UNSUPPORTED_SPEC, null);
  }
}
