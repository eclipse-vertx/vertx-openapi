package io.vertx.openapi.contract;

import static io.vertx.openapi.contract.ContractErrorType.INVALID_SPEC;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_SPEC;

public class OpenAPIContractException extends RuntimeException {

  private final ContractErrorType type;

  public OpenAPIContractException(String message, ContractErrorType type, Throwable cause) {
    super(message, cause);
    this.type = type;
  }

  public static OpenAPIContractException createInvalidContract(String reason) {
    return createInvalidContract(reason, null);
  }

  public static OpenAPIContractException createInvalidContract(String reason, Throwable cause) {
    String msg = "The passed OpenAPI contract is invalid" + (reason == null ? "." : ": " + reason);
    return new OpenAPIContractException(msg, INVALID_SPEC, cause);
  }

  public static OpenAPIContractException createUnsupportedVersion(String version) {
    return new OpenAPIContractException("The version of the passed OpenAPI contract is not supported: " + version,
      UNSUPPORTED_SPEC, null);
  }

  public ContractErrorType type() {
    return type;
  }
}
