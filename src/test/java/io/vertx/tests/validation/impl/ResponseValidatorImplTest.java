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

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Response;
import io.vertx.openapi.validation.ResponseParameter;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import io.vertx.openapi.validation.impl.ResponseValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.tests.MockHelper.mockParameter;
import static io.vertx.tests.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.openapi.contract.Location.HEADER;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class ResponseValidatorImplTest {
  private ResponseValidatorImpl validator;
  private OpenAPIContract contractSpy;

  private static Parameter buildParam(String name, JsonObject schema, boolean required) {
    return mockParameter(name, HEADER, SIMPLE, false, JsonSchema.of(schema), required);
  }

  private static Stream<Arguments> provideNullRequestParameters() {
    return Stream.of(
      Arguments.of("RequestParameter is null", null),
      Arguments.of("Object in RequestParameter is null", new RequestParameterImpl(null))
    );
  }

  private static Response mockResponse() {
    MediaType mockedMediaType = mock(MediaType.class);
    when(mockedMediaType.getSchema()).thenReturn(JsonSchema.of(objectSchema().toJson()));

    Response mockedResponse = mock(Response.class);
    when(mockedResponse.getContent()).thenReturn(ImmutableMap.of(APPLICATION_JSON.toString(), mockedMediaType));

    return mockedResponse;
  }

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract).onSuccess(c -> {
      this.contractSpy = spy(c);
      this.validator = new ResponseValidatorImpl(vertx, contractSpy);
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidate(VertxTestContext testContext) {
    Map<String, String> headers = ImmutableMap.of("x-next", "foo", "ignore", "this");

    JsonArray body = new JsonArray().add(new JsonObject().put("id", 1337).put("name", "foo"));

    ValidatableResponse validatableResponse =
      ValidatableResponse.create(200, headers, body.toBuffer(), APPLICATION_JSON.toString());

    validator.validate(validatableResponse, "listPets")
      .onComplete(testContext.succeeding(validated -> testContext.verify(() -> {
        assertThat(validated.getBody().getJsonArray()).isEqualTo(body);
        assertThat(validated.getHeaders()).hasSize(1);
        assertThat(validated.getHeaders().get("x-next").getString()).isEqualTo("foo");
        testContext.completeNow();
      })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testGetResponseThrowResponseNotFound(VertxTestContext testContext) {
    String operationId = "isMocked";
    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getDefaultResponse()).thenReturn(null);
    when(mockedOperation.getResponse(anyInt())).thenReturn(null);
    when(contractSpy.operation(operationId)).thenReturn(mockedOperation);

    validator.getResponse(ValidatableResponse.create(1337), operationId).onFailure(t -> testContext.verify(() -> {
      assertThat(t).isInstanceOf(ValidatorException.class);
      assertThat(t).hasMessageThat().isEqualTo("No response defined for status code 1337 in Operation isMocked");
      testContext.completeNow();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testGetResponse(VertxTestContext testContext) {
    String operationId = "isMocked";

    Response mockedResponse = mock(Response.class);
    Response mockedDefaultResponse = mock(Response.class);

    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getDefaultResponse()).thenReturn(mockedDefaultResponse);
    when(mockedOperation.getResponse(1337)).thenReturn(mockedResponse);
    when(contractSpy.operation(operationId)).thenReturn(mockedOperation);

    Checkpoint cp = testContext.checkpoint(2);

    validator.getResponse(ValidatableResponse.create(1337), operationId)
      .onComplete(testContext.succeeding(resp -> testContext.verify(() -> {
        assertThat(resp).isEqualTo(mockedResponse);
        cp.flag();
      })));
    validator.getResponse(ValidatableResponse.create(0), operationId)
      .onComplete(testContext.succeeding(resp -> testContext.verify(() -> {
        assertThat(resp).isEqualTo(mockedDefaultResponse);
        cp.flag();
      })));
  }

  @Test
  void testValidateParameter() {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    ResponseParameter validated = validator.validateParameter(param, new RequestParameterImpl("5"));
    assertThat(validated.getInteger()).isEqualTo(5);
  }

  static Stream<Arguments> testValidateParameterThrowInvalidValue() {
    return Stream.of(
      Arguments.of(numberSchema(), true, "Instance type boolean is invalid. Expected number"),
      Arguments.of(booleanSchema(), "{}", "Instance type object is invalid. Expected boolean"),
      Arguments.of(booleanSchema(), "3", "Instance type number is invalid. Expected boolean")
    );
  }

  @ParameterizedTest(name = "{index} Throw invalid value error for [{1}]")
  @MethodSource
  void testValidateParameterThrowInvalidValue(SchemaBuilder<?, ?> schema, Object value, String reason) {
    Parameter param = buildParam("p1", schema.toJson(), false);
    ValidatorException exception =
      assertThrows(
        ValidatorException.class,
        () -> validator.validateParameter(param, new RequestParameterImpl(value)));

    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String expectedMsg = "The value of header parameter p1 is invalid. Reason: " + reason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Throw error when required param is missing: {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateParameterThrowRequired(String scenario, ResponseParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validateParameter(param, value));
    String expectedMsg = "The related request / response does not contain the required header parameter p1";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Return empty ResponseParameter when param is missing but not required: {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateParameterValueNullNotRequired(String scenario, ResponseParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), false);
    ResponseParameter validated = validator.validateParameter(param, value);
    assertThat(validated.isNull()).isTrue();
  }

  @Test
  void testValidateBodyNoContent() {
    Response mockedResponse = mock(Response.class);
    when(mockedResponse.getContent()).thenReturn(Collections.emptyMap());
    ResponseParameter validated = validator.validateBody(mockedResponse, null);
    assertThat(validated.isNull()).isTrue();
  }

  @ParameterizedTest(name = "{index} If body is required, validateBody should throw an error if {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateBodyRequiredButNullOrEmpty(String scenario, ResponseParameter parameter) {
    Response mockedResponse = mockResponse();
    ValidatableResponse mockedValidatableResponse = mock(ValidatableResponse.class);
    when(mockedValidatableResponse.getBody()).thenReturn(parameter);

    ValidatorException exceptionEmpty =
      assertThrows(ValidatorException.class,
        () -> validator.validateBody(mockedResponse, mockedValidatableResponse));
    assertThat(exceptionEmpty.type()).isEqualTo(MISSING_REQUIRED_PARAMETER);
    String expectedMsg = "The related response does not contain the required body.";
    assertThat(exceptionEmpty).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "validateBody should throw an error if MediaType or Transformer is null")
  @ValueSource(strings = {"text/plain", "foo/bar"})
  void testValidateBodyMediaTypeOrTransformerNull(String contentType) {
    Response mockedResponse = mock(Response.class);
    when(mockedResponse.getContent()).thenReturn(ImmutableMap.of(TEXT_PLAIN.toString(), mock(MediaType.class)));

    ValidatableResponse mockedValidatableResponse = mock(ValidatableResponse.class);
    when(mockedValidatableResponse.getContentType()).thenReturn(contentType);
    when(mockedValidatableResponse.getBody()).thenReturn(new RequestParameterImpl("foo"));

    ValidatorException exceptionEmpty =
      assertThrows(ValidatorException.class,
        () -> validator.validateBody(mockedResponse, mockedValidatableResponse));
    assertThat(exceptionEmpty.type()).isEqualTo(UNSUPPORTED_VALUE_FORMAT);
    String expectedMsg = "The format of the response body is not supported";
    assertThat(exceptionEmpty).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testValidateBodyThrowInvalidValue() {
    Response mockedResponse = mockResponse();
    ValidatableResponse mockedValidatableResponse = mock(ValidatableResponse.class);
    when(mockedValidatableResponse.getBody()).thenReturn(new RequestParameterImpl(Buffer.buffer("3")));
    when(mockedValidatableResponse.getContentType()).thenReturn(APPLICATION_JSON.toString());

    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validateBody(mockedResponse, mockedValidatableResponse));
    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String reason = "Instance type number is invalid. Expected object";
    String expectedMsg = "The value of the response body is invalid. Reason: " + reason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testValidateBody() {
    JsonObject body = new JsonObject().put("foo", "bar");

    Response mockedResponse = mockResponse();
    ValidatableResponse mockedValidatableResponse = mock(ValidatableResponse.class);
    when(mockedValidatableResponse.getBody()).thenReturn(new RequestParameterImpl(body.toBuffer()));
    when(mockedValidatableResponse.getContentType()).thenReturn(APPLICATION_JSON.toString());

    ResponseParameter validated = validator.validateBody(mockedResponse, mockedValidatableResponse);
    assertThat(validated.getJsonObject()).isEqualTo(body);
  }
}
