package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public enum ContractErrorType {
  /**
   * Provided file is not a valid OpenAPI contract
   */
  INVALID_SPEC,
  /**
   * You are trying to use an OpenAPI contract within a version that is not supported.
   */
  UNSUPPORTED_SPEC,
  /**
   * You are trying to use an OpenAPI feature that is not supported.
   */
  UNSUPPORTED_FEATURE
}
