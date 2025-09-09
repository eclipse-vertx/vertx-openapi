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

package io.vertx.tests.validation.impl;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.tests.ResourceHelper.TEST_RESOURCE_PATH;
import static java.util.Collections.emptyMap;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.impl.MediaTypeImpl;
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.impl.BaseValidator;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
class BaseValidatorTest {
  private BaseValidatorWrapper validator;

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract).onSuccess(c -> testContext.verify(() -> {
      this.validator = new BaseValidatorWrapper(vertx, c);
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testGetOperation(VertxTestContext testContext) {
    String operationId = "listPets";
    validator.getOperation(operationId).onFailure(testContext::failNow)
        .onSuccess(operation -> testContext.verify(() -> {
          assertThat(operation.getOperationId()).isEqualTo(operationId);
          testContext.completeNow();
        }));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testGetOperationThrow(VertxTestContext testContext) {
    validator.getOperation("invalidId").onFailure(t -> testContext.verify(() -> {
      assertThat(t).isInstanceOf(ValidatorException.class);
      assertThat(t).hasMessageThat().isEqualTo("Invalid OperationId: invalidId");
      testContext.completeNow();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  static Stream<Arguments> testIsSchemaValidationRequired() {
    JsonObject stringSchema = new JsonObject().put("type", "string");
    JsonObject binaryStringSchema = stringSchema.copy().put("format", "binary");
    Function<JsonObject, JsonObject> buildMediaModel = schema -> new JsonObject().put("schema", schema);

    MediaType noMediaModel = new MediaTypeImpl("", EMPTY_JSON_OBJECT, emptyMap());
    MediaType typeNumber = new MediaTypeImpl("", buildMediaModel.apply(new JsonObject().put("type", "number")), emptyMap());
    MediaType typeStringNoFormat = new MediaTypeImpl("", buildMediaModel.apply(stringSchema), emptyMap());
    MediaType typeStringFormatBinary = new MediaTypeImpl("", buildMediaModel.apply(binaryStringSchema), emptyMap());
    MediaType typeStringFormatTime = new MediaTypeImpl("", buildMediaModel.apply(stringSchema.copy().put("format",
        "time")), emptyMap());
    MediaType typeStringFormatBinaryMinLength = new MediaTypeImpl("",
        buildMediaModel.apply(binaryStringSchema.copy().put("minLength", 1)), emptyMap());

    return Stream.of(
        Arguments.of("No media model is defined", noMediaModel, false),
        Arguments.of("Type number", typeNumber, true),
        Arguments.of("Type String without format", typeStringNoFormat, true),
        Arguments.of("Type String and format binary", typeStringFormatBinary, false),
        Arguments.of("Type String and format time", typeStringFormatTime, true),
        Arguments.of("Type String and format binary but minLength", typeStringFormatBinaryMinLength, true));
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource
  void testIsSchemaValidationRequired(String scenario, MediaType mediaType, boolean isRequired) {
    assertThat(validator.isSchemaValidationRequired(mediaType)).isEqualTo(isRequired);
  }

  private static class BaseValidatorWrapper extends BaseValidator {

    public BaseValidatorWrapper(Vertx vertx, OpenAPIContract contract) {
      super(vertx, contract);
    }

    @Override
    protected Future<Operation> getOperation(String operationId) {
      return super.getOperation(operationId);
    }

    @Override
    protected boolean isSchemaValidationRequired(MediaType mediaType) {
      return super.isSchemaValidationRequired(mediaType);
    }

    @Override
    protected RequestParameterImpl validate(MediaType mediaType, String contentType, Buffer rawContent,
        ValidationContext requestOrResponse) {
      return super.validate(mediaType, contentType, rawContent, requestOrResponse);
    }
  }
}
