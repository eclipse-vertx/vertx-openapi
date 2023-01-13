package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;

@VertxGen
public enum ValidatorErrorType {
  /**
   * A required parameter was not part of the request
   */
  MISSING_REQUIRED_PARAMETER,

  /**
   * The format of the related value does not fit to the expected {@link io.vertx.openapi.contract.Style}
   */
  INVALID_VALUE_FORMAT,

  /**
   * The format of the related value is not yet supported.
   */
  UNSUPPORTED_VALUE_FORMAT,

  /**
   * The value of the related parameter can't be decoded.
   */
  ILLEGAL_VALUE,

  /**
   * The value of the related parameter does not fit to the schema.
   */
  INVALID_VALUE
}
