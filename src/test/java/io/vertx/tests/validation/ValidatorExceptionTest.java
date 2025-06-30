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

package io.vertx.tests.validation;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.tests.MockHelper.mockParameter;

import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorErrorType;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;

class ValidatorExceptionTest {

  private static final Parameter DUMMY_PARAMETER =
      mockParameter("dummy", PATH, LABEL, false, JsonSchema.of(intSchema().toJson()));

  @Test
  void testCreateMissingRequiredParameter() {
    ValidatorException exception = ValidatorException.createMissingRequiredParameter(DUMMY_PARAMETER);
    String expectedMsg = "The related request / response does not contain the required path parameter dummy";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
  }

  @Test
  void testCreateInvalidValueFormat() {
    ValidatorException exception = ValidatorException.createInvalidValueFormat(DUMMY_PARAMETER);
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style label.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.INVALID_VALUE_FORMAT);
  }

  @Test
  void testCreateUnsupportedValueFormat() {
    ValidatorException exception = ValidatorException.createUnsupportedValueFormat(DUMMY_PARAMETER);
    String expectedMsg = "Values in style label with exploded=false are not supported for path parameter dummy.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT);
  }

  @Test
  void testCreateCantDecodeValue() {
    ValidatorException exception = ValidatorException.createCantDecodeValue(DUMMY_PARAMETER);
    String expectedMsg = "The value of path parameter dummy can't be decoded.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.ILLEGAL_VALUE);
  }

  @Test
  void testCreateOperationIdInvalid() {
    ValidatorException exception = ValidatorException.createOperationIdInvalid("getPets");
    String expectedMsg = "Invalid OperationId: getPets";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_OPERATION);
  }

  @Test
  void testCreateOperationNotFound() {
    ValidatorException exception = ValidatorException.createOperationNotFound(GET, "/my/path");
    String expectedMsg = "No operation found for the request: GET /my/path";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_OPERATION);
  }

  @Test
  void testCreateResponseNotFound() {
    ValidatorException exception = ValidatorException.createResponseNotFound(1337, "getPets");
    String expectedMsg = "No response defined for status code 1337 in Operation getPets";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_RESPONSE);
  }
}
