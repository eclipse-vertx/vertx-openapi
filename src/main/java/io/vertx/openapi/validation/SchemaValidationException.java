/*
 * Copyright (c) 2024, SAP SE
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

import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.Parameter;

import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;

/**
 * A SchemaValidationException is a special case of a {@link ValidatorException} and is thrown, if the validation of a
 * request or response fails due to a schema violation. It offers access to the related OutputUnit, which can be used to
 * gain more information about the validation error.
 */
public class SchemaValidationException extends ValidatorException {

  private final OutputUnit outputUnit;

  public SchemaValidationException(String message, ValidatorErrorType type, OutputUnit outputUnit, Throwable cause) {
    super(message, type, cause);
    this.outputUnit = outputUnit;
  }

  public static SchemaValidationException createInvalidValueParameter(Parameter parameter, OutputUnit outputUnit,
                                                                      JsonSchemaValidationException cause) {
    String msg = String.format("The value of %s parameter %s is invalid. Reason: %s",
      parameter.getIn().name().toLowerCase(), parameter.getName(), extractReason(outputUnit));
    return new SchemaValidationException(msg, INVALID_VALUE, outputUnit, cause);
  }

  public static SchemaValidationException createInvalidValueRequestBody(OutputUnit outputUnit,
                                                                        JsonSchemaValidationException cause) {
    String msg = String.format("The value of the request body is invalid. Reason: %s", extractReason(outputUnit));
    return new SchemaValidationException(msg, INVALID_VALUE, outputUnit, cause);
  }

  public static SchemaValidationException createInvalidValueResponseBody(OutputUnit outputUnit,
                                                                         JsonSchemaValidationException cause) {
    String msg = String.format("The value of the response body is invalid. Reason: %s", extractReason(outputUnit));
    return new SchemaValidationException(msg, INVALID_VALUE, outputUnit, cause);
  }


  public static SchemaValidationException createMissingValueRequestBody(OutputUnit outputUnit,
                                                                         JsonSchemaValidationException cause) {
    String msg = String.format("The value of the request body is missing. Reason: %s", extractReason(outputUnit));
    return new SchemaValidationException(msg, MISSING_REQUIRED_PARAMETER, outputUnit, cause);
  }

  public static SchemaValidationException createErrorFromOutputUnitType(Parameter parameter, OutputUnit outputUnit,
                                                                        JsonSchemaValidationException cause) {
    switch(outputUnit.getErrorType()) {
      case MISSING_VALUE:
        return createMissingValueRequestBody(outputUnit, cause);
      case INVALID_VALUE:
      case NONE:
      default:
        return createInvalidValueParameter(parameter, outputUnit, cause);
    }
  }

  /**
   * Returns the related OutputUnit of the validation error.
   *
   * @return The related OutputUnit of the validation error.
   */
  public OutputUnit getOutputUnit() {
    return outputUnit;
  }

  // VisibleForTesting
  static String extractReason(OutputUnit outputUnit) {
    // Errors can't be empty, because we ran into an error
    OutputUnit mostRelevant = outputUnit.getErrors().get(outputUnit.getErrors().size() - 1);
    String location = mostRelevant.getInstanceLocation();

    return mostRelevant.getError() + (location.length() > 1 ? " at " + location : "");
  }
}
