/*
 * Copyright (c) 2023, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.openapi.validation;

import io.vertx.core.http.HttpMethod;
import io.vertx.openapi.contract.Parameter;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE_FORMAT;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_OPERATION;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_RESPONSE;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;

/**
 * A ValidatorException is thrown, if the validation of a request or response fails. The validation can fail for
 * formal reasons, such as the wrong format for a parameter or the absence of a required parameter. However,
 * validation can of course also fail because the content does not match the defined schema. In this case
 * have a look into {@link SchemaValidationException}.
 */
public class ValidatorException extends RuntimeException {

  private final ValidatorErrorType type;

  public ValidatorException(String message, ValidatorErrorType type) {
    this(message, type, null);
  }

  protected ValidatorException(String message, ValidatorErrorType type, Throwable cause) {
    super(message, cause);
    this.type = type;
  }

  public static ValidatorException createMissingRequiredParameter(Parameter parameter) {
    String msg = String.format("The related request / response does not contain the required %s parameter %s",
      parameter.getIn().name().toLowerCase(), parameter.getName());
    return new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
  }

  public static ValidatorException createInvalidValueFormat(Parameter parameter) {
    String msg = String.format("The formatting of the value of %s parameter %s doesn't match to style %s.",
      parameter.getIn().name().toLowerCase(), parameter.getName(), parameter.getStyle());
    return new ValidatorException(msg, INVALID_VALUE_FORMAT);
  }

  public static ValidatorException createUnsupportedValueFormat(Parameter parameter) {
    String msg =
      String.format("Values in style %s with exploded=%s are not supported for %s parameter %s.", parameter.getStyle(),
        parameter.isExplode(), parameter.getIn().name().toLowerCase(), parameter.getName());
    return new ValidatorException(msg, UNSUPPORTED_VALUE_FORMAT);
  }

  public static ValidatorException createCantDecodeValue(Parameter parameter) {
    String msg = String.format("The value of %s parameter %s can't be decoded.", parameter.getIn().name().toLowerCase(),
      parameter.getName());
    return new ValidatorException(msg, ILLEGAL_VALUE);
  }

  public static ValidatorException createOperationIdInvalid(String operationId) {
    String msg = String.format("Invalid OperationId: %s", operationId);
    return new ValidatorException(msg, MISSING_OPERATION);
  }

  public static ValidatorException createOperationNotFound(HttpMethod method, String path) {
    String msg = String.format("No operation found for the request: %s %s", method.name(), path);
    return new ValidatorException(msg, MISSING_OPERATION);
  }

  public static ValidatorException createResponseNotFound(int statusCode, String operation) {
    String msg = String.format("No response defined for status code %s in Operation %s", statusCode, operation);
    return new ValidatorException(msg, MISSING_RESPONSE);
  }

  public ValidatorErrorType type() {
    return type;
  }
}
