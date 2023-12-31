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
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.impl.ParameterImpl;

import java.util.Optional;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE_FORMAT;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_OPERATION;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_RESPONSE;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;

public class ValidatorException extends RuntimeException {

  private final ValidatorErrorType type;

  public ValidatorException(String message, ValidatorErrorType type) {
    this(message, type, null);
  }

  public ValidatorException(String message, ValidatorErrorType type, Throwable cause) {
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

  public static ValidatorException createInvalidValue(Parameter parameter, JsonSchemaValidationException cause) {
    String msg = String.format("The value of %s parameter %s is invalid. Reason: %s",
      parameter.getIn().name().toLowerCase(), parameter.getName(), extractReason(cause));
    return new ValidatorException(msg, INVALID_VALUE, cause);
  }

  public static ValidatorException createInvalidValueBody(JsonSchemaValidationException cause) {
    String msg = String.format("The value of the request / response body is invalid. Reason: %s", extractReason(cause));
    return new ValidatorException(msg, INVALID_VALUE, cause);
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

  public static ValidatorException createParameterFormatInvalid(Parameter parameter, Object input, String format) {
    String msg = String.format("The value of %s parameter %s is invalid. Reason: The format %s doesn't match the input format: %s",
      parameter.getIn().name().toLowerCase(), parameter.getName(), format, input != null ? input.getClass().getSimpleName() : "null");
    return new ValidatorException(msg, INVALID_VALUE_FORMAT);
  }

  public static ValidatorException createParameterFormatInvalidDueToInfinite(Parameter parameter, String format) {
    String msg = String.format("The value of %s parameter %s is invalid. Reason: The format %s doesn't match the input format: %s",
      parameter.getIn().name().toLowerCase(), parameter.getName(), format, "infinite");
    return new ValidatorException(msg, INVALID_VALUE_FORMAT);
  }

  static String extractReason(JsonSchemaValidationException e) {
    // Workaround until JsonSchemaValidationException provides instanceLocation
    String location =
      Optional.ofNullable(e.getStackTrace()).map(elements -> elements[0]).map(StackTraceElement::getMethodName)
        .map(s -> s.substring(1, s.length() - 1)).orElse(null);
    if (location == null) {
      int hashTag = e.location().indexOf('#');
      location = hashTag < 0 ? e.location() : e.location().substring(hashTag);
    }
    return e.getMessage() + (location.length() > 1 ? " at " + location : "");
  }

  public ValidatorErrorType type() {
    return type;
  }
}
