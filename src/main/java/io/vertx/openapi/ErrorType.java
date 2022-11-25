package io.vertx.openapi;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public enum ErrorType {
  /**
   * You are trying to mount an operation with operation_id not defined in specification
   */
  OPERATION_ID_NOT_FOUND,
  /**
   * Provided file is not a valid OpenAPI contract
   */
  INVALID_SPEC,
  /**
   * You are trying to use an OpenAPI contract within a version that is not supported.
   */
  UNSUPPORTED_SPEC
}
