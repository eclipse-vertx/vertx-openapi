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

package io.vertx.tests.validation;

import com.google.common.collect.ImmutableList;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.OutputErrorType;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.SchemaValidationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.tests.MockHelper.mockParameter;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;

class SchemaValidationExceptionTest {

  private static final Parameter DUMMY_PARAMETER =
    mockParameter("dummy", PATH, LABEL, false, JsonSchema.of(intSchema().toJson()));

  private static final JsonSchemaValidationException DUMMY_CAUSE = new JsonSchemaValidationException("dummy",
    new Exception(), "dummyLocation", OutputErrorType.INVALID_VALUE);
  private static final OutputUnit DUMMY_ERROR_UNIT = new OutputUnit("instanceLocation", "absoluteKeywordLocation",
    "keywordLocation", "error", OutputErrorType.INVALID_VALUE);
  private static final OutputUnit DUMMY_OUTPUT_UNIT = new OutputUnit("instanceLocation2", "absoluteKeywordLocation2",
    "keywordLocation2", "error2", OutputErrorType.MISSING_VALUE);
  private static final OutputUnit DUMMY_OUTPUT_UNIT_INVALID = new OutputUnit("instanceLocation2", "absoluteKeywordLocation2",
    "keywordLocation2", "error2", OutputErrorType.INVALID_VALUE);

  @BeforeAll
  static void setup() {
    DUMMY_OUTPUT_UNIT.setErrors(ImmutableList.of(DUMMY_ERROR_UNIT));
    DUMMY_OUTPUT_UNIT_INVALID.setErrors(ImmutableList.of(DUMMY_ERROR_UNIT));
  }

  @Test
  void testCreateInvalidValueParameter() {
    SchemaValidationException exception = SchemaValidationException.createInvalidValueParameter(DUMMY_PARAMETER,
      DUMMY_OUTPUT_UNIT, DUMMY_CAUSE);
    assertThat(exception.getOutputUnit()).isEqualTo(DUMMY_OUTPUT_UNIT);
    assertThat(exception.getCause()).isEqualTo(DUMMY_CAUSE);
    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String expectedMsg = "The value of path parameter dummy is invalid. Reason: error at instanceLocation";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testCreateInvalidValueRequestBody() {
    SchemaValidationException exception = SchemaValidationException.createInvalidValueRequestBody(DUMMY_OUTPUT_UNIT,
      DUMMY_CAUSE);
    assertThat(exception.getOutputUnit()).isEqualTo(DUMMY_OUTPUT_UNIT);
    assertThat(exception.getCause()).isEqualTo(DUMMY_CAUSE);
    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String excpectedMsg = "The value of the request body is invalid. Reason: error at instanceLocation";
    assertThat(exception).hasMessageThat().isEqualTo(excpectedMsg);
  }

  @Test
  void testCreateInvalidValueResponseBody() {
    SchemaValidationException exception = SchemaValidationException.createInvalidValueResponseBody(DUMMY_OUTPUT_UNIT,
      DUMMY_CAUSE);
    assertThat(exception.getOutputUnit()).isEqualTo(DUMMY_OUTPUT_UNIT);
    assertThat(exception.getCause()).isEqualTo(DUMMY_CAUSE);
    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String excpectedMsg = "The value of the response body is invalid. Reason: error at instanceLocation";
    assertThat(exception).hasMessageThat().isEqualTo(excpectedMsg);
  }

  @Test
  void testCreateMissingValueRequestBody() {
    SchemaValidationException exception = SchemaValidationException.createMissingValueRequestBody(DUMMY_OUTPUT_UNIT,
      DUMMY_CAUSE);

    assertThat(exception.getOutputUnit()).isEqualTo(DUMMY_OUTPUT_UNIT);
    assertThat(exception.getCause()).isEqualTo(DUMMY_CAUSE);
    assertThat(exception.type()).isEqualTo(MISSING_REQUIRED_PARAMETER);
    String excpectedMsg = "The value of the request body is missing. Reason: error at instanceLocation";
    assertThat(exception).hasMessageThat().isEqualTo(excpectedMsg);
  }

  @Test
  void testCreateErrorFromOutputUnitType() {

    SchemaValidationException exception = SchemaValidationException.createErrorFromOutputUnitType(DUMMY_PARAMETER,
      DUMMY_OUTPUT_UNIT, DUMMY_CAUSE);

    assertThat(exception.getOutputUnit()).isEqualTo(DUMMY_OUTPUT_UNIT);
    assertThat(exception.getCause()).isEqualTo(DUMMY_CAUSE);
    assertThat(exception.type()).isEqualTo(MISSING_REQUIRED_PARAMETER);
    String excpectedMsg = "The value of the request body is missing. Reason: error at instanceLocation";
    assertThat(exception).hasMessageThat().isEqualTo(excpectedMsg);

    SchemaValidationException exception_invalid = SchemaValidationException.createErrorFromOutputUnitType(DUMMY_PARAMETER,
      DUMMY_OUTPUT_UNIT_INVALID, DUMMY_CAUSE);

    assertThat(exception_invalid.getOutputUnit()).isEqualTo(DUMMY_OUTPUT_UNIT_INVALID);
    assertThat(exception_invalid.getCause()).isEqualTo(DUMMY_CAUSE);
    assertThat(exception_invalid.type()).isEqualTo(INVALID_VALUE);
    excpectedMsg = "The value of path parameter dummy is invalid. Reason: error at instanceLocation";
    assertThat(exception_invalid).hasMessageThat().isEqualTo(excpectedMsg);
  }

}
