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

package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.RequestBody;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.openapi.contract.Location.COOKIE;
import static io.vertx.openapi.contract.Location.HEADER;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class RequestValidatorImplTest {

  private RequestValidatorImpl validator;

  private OpenAPIContract contractSpy;

  private static Parameter buildParam(String name, JsonObject schema, boolean required) {
    return buildParam(name, PATH, SIMPLE, schema, required);
  }

  private static Parameter buildParam(String name, Location in, Style style, JsonObject schema, boolean required) {
    return mockParameter(name, in, style, false, JsonSchema.of(schema), required);
  }

  private static Stream<Arguments> provideNullRequestParameters() {
    return Stream.of(
      Arguments.of("RequestParameter is null", null),
      Arguments.of("Object in RequestParameter is null", new RequestParameterImpl(null))
    );
  }

  private static Stream<Arguments> testValidateWithValidatableRequestAndOperationId() {
    List<Parameter> parameters = new ArrayList<>();
    parameters.add(buildParam("CookieParamAge", COOKIE, FORM, intSchema().toJson(), true));
    parameters.add(buildParam("HeaderParamUser", HEADER, SIMPLE, objectSchema().toJson(), true));
    parameters.add(buildParam("PathParamVersion", PATH, SIMPLE, numberSchema().toJson(), true));
    parameters.add(buildParam("QueryParamTrace", QUERY, FORM, booleanSchema().toJson(), true));

    MediaType mockedMediaType = mock(MediaType.class);
    when(mockedMediaType.getSchema()).thenReturn(JsonSchema.of(objectSchema().toJson()));

    RequestBody mockedRequestBody = mock(RequestBody.class);
    when(mockedRequestBody.isRequired()).thenReturn(true);
    when(mockedRequestBody.getContent()).thenReturn(ImmutableMap.of(APPLICATION_JSON.toString(), mockedMediaType));

    JsonObject body = new JsonObject().put("foo", "bar");

    Map<String, RequestParameter> expectedCookies =
      ImmutableMap.of("CookieParamAge", new RequestParameterImpl(1337));
    Map<String, RequestParameter> rawCookies = ImmutableMap.of("CookieParamAge", new RequestParameterImpl("1337"));

    Map<String, RequestParameter> expectedHeaderParameters =
      ImmutableMap.of("HeaderParamUser",
        new RequestParameterImpl(new JsonObject().put("name", "foo").put("id", 9001)));
    Map<String, RequestParameter> rawHeaderParameters =
      ImmutableMap.of("HeaderParamUser", new RequestParameterImpl("name,foo,id,9001"));

    Map<String, RequestParameter> expectedPathParameters =
      ImmutableMap.of("PathParamVersion", new RequestParameterImpl(4.2));
    Map<String, RequestParameter> rawPathParameters =
      ImmutableMap.of("PathParamVersion", new RequestParameterImpl("4.2"));

    Map<String, RequestParameter> expectedQuery =
      ImmutableMap.of("QueryParamTrace", new RequestParameterImpl(true));
    Map<String, RequestParameter> rawQuery = ImmutableMap.of("QueryParamTrace", new RequestParameterImpl("true"));

    ValidatedRequest expected =
      new ValidatedRequestImpl(expectedCookies, expectedHeaderParameters, expectedPathParameters, expectedQuery,
        new RequestParameterImpl(body));
    ValidatableRequest request =
      new ValidatableRequestImpl(rawCookies, rawHeaderParameters, rawPathParameters, rawQuery,
        new RequestParameterImpl(body.toBuffer()),
        APPLICATION_JSON.toString());

    return Stream.of(
      Arguments.of(parameters, mockedRequestBody, request, expected)
    );
  }

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract).onSuccess(c -> {
      this.contractSpy = spy(c);
      this.validator = new RequestValidatorImpl(vertx, contractSpy);
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void voidTestValidateWithRequest(VertxTestContext testContext) {
    String operationId = "isMocked";
    HttpServerRequest requestMock = mock(HttpServerRequest.class);
    when(requestMock.path()).thenReturn("/mocked/path");
    when(requestMock.method()).thenReturn(GET);

    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getParameters()).thenReturn(emptyList());
    when(mockedOperation.getOperationId()).thenReturn(operationId);

    when(contractSpy.findOperation("/mocked/path", GET)).thenReturn(mockedOperation);
    when(contractSpy.operation(operationId)).thenReturn(mockedOperation);

    RequestValidator validatorSpy = spy(validator);
    validatorSpy.validate(requestMock).onSuccess(v -> testContext.verify(() -> {
      verify(validatorSpy).validate(isA(HttpServerRequest.class), eq(operationId));
      verify(validatorSpy).validate(isA(ValidatableRequest.class), eq(operationId));
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void voidTestValidateWithRequestAndOperationId(VertxTestContext testContext) {
    RequestValidator validatorSpy = spy(validator);
    HttpServerRequest requestMock = mock(HttpServerRequest.class);
    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getParameters()).thenReturn(emptyList());
    when(contractSpy.operation(anyString())).thenReturn(mockedOperation);

    validatorSpy.validate(requestMock, "isMocked").onSuccess(v -> testContext.verify(() -> {
      verify(validatorSpy).validate(isA(ValidatableRequest.class), eq("isMocked"));
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @ParameterizedTest
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateWithValidatableRequestAndOperationId(List<Parameter> parameters, RequestBody requestBody,
    ValidatableRequest request, ValidatedRequest expected, VertxTestContext testContext) {
    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getParameters()).thenReturn(parameters);
    when(mockedOperation.getRequestBody()).thenReturn(requestBody);
    when(contractSpy.operation(anyString())).thenReturn(mockedOperation);

    validator.validate(request, "isMocked").onSuccess(validatedParams -> testContext.verify(() -> {
      assertThat(validatedParams.getHeaders()).containsExactlyEntriesIn(expected.getHeaders());
      assertThat(validatedParams.getCookies()).containsExactlyEntriesIn(expected.getCookies());
      assertThat(validatedParams.getPathParameters()).containsExactlyEntriesIn(expected.getPathParameters());
      assertThat(validatedParams.getQuery()).containsExactlyEntriesIn(expected.getQuery());
      assertThat(validatedParams.getBody()).isEqualTo(expected.getBody());
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateThrowOperationIdInValid(VertxTestContext testContext) {
    Checkpoint cp = testContext.checkpoint(2);
    validator.validate((HttpServerRequest) null, "invalidId").onFailure(t -> testContext.verify(() -> {
      assertThat(t).hasMessageThat().isEqualTo("Invalid OperationId: invalidId");
      cp.flag();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));

    validator.validate((ValidatableRequest) null, "invalidId").onFailure(t -> testContext.verify(() -> {
      assertThat(t).hasMessageThat().isEqualTo("Invalid OperationId: invalidId");
      cp.flag();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateThrowOperationNotFound(VertxTestContext testContext) {
    HttpServerRequest requestMock = mock(HttpServerRequest.class);
    when(requestMock.path()).thenReturn("/invalid/path");
    when(requestMock.method()).thenReturn(GET);

    validator.validate(requestMock).onFailure(t -> testContext.verify(() -> {
      assertThat(t).hasMessageThat().isEqualTo("No operation found for the request: GET /invalid/path");
      testContext.completeNow();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateCatchErrorFromTransformer(VertxTestContext testContext) {
    Checkpoint cp = testContext.checkpoint(2);

    List<Parameter> parameters = new ArrayList<>();
    parameters.add(buildParam("HeaderParamUser", HEADER, SIMPLE, objectSchema().toJson(), true));
    Map<String, RequestParameter> rawHeaderParameters =
      ImmutableMap.of("HeaderParamUser", new RequestParameterImpl("name,foo,id"));

    ValidatableRequest request =
      new ValidatableRequestImpl(null, rawHeaderParameters, null, null, null, null);

    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getParameters()).thenReturn(parameters);
    when(contractSpy.operation(anyString())).thenReturn(mockedOperation);

    String expected = "The formatting of the value of header parameter HeaderParamUser doesn't match to style simple.";
    validator.validate(request, "isMocked").onFailure(e -> testContext.verify(() -> {
      assertThat(e).hasMessageThat().isEqualTo(expected);
      cp.flag();
    })).onComplete(testContext.failing(validatedParams -> cp.flag()));
  }

  @Test
  void testValidateParameter() {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    RequestParameter validated = validator.validateParameter(param, new RequestParameterImpl("5"));
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
  void testValidateParameterThrowInvalidValue(SchemaBuilder<?,?> schema, Object value, String reason) {
    Parameter param = buildParam("p1", schema.toJson(), false);
    ValidatorException exception =
        assertThrows(
            ValidatorException.class,
            () -> validator.validateParameter(param, new RequestParameterImpl(value)));

    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String expectedMsg = "The value of path parameter p1 is invalid. Reason: " + reason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testStringParameterValidatesNotAsIntegerType() {
    Parameter param = buildParam("p1", stringSchema().toJson(), false);
    RequestParameter validated = validator.validateParameter(param, new RequestParameterImpl("3"));
    assertThat(validated.getString()).isEqualTo("3");
  }

  @ParameterizedTest(name = "{index} Throw error when required param is missing: {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateParameterThrowRequired(String scenario, RequestParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validateParameter(param, value));
    String expectedMsg = "The related request / response does not contain the required path parameter p1";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Return RequestParameter with null, if parameter is missing and not required. {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateParameterMissingParamIsNotRequired(String scenario, RequestParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), false);
    assertThat(validator.validateParameter(param, value).isEmpty()).isTrue();
  }

  @ParameterizedTest(name = "{index} Throw UNSUPPORTED_VALUE_FORMAT error when param style is {0}")
  @EnumSource(value = Style.class, names = {"SPACE_DELIMITED", "PIPE_DELIMITED", "DEEP_OBJECT"})
  void testValidateParameterThrowUnsupportedValueFormat(Style style) {
    Parameter param = mockParameter("dummy", HEADER, style, false, JsonSchema.of(stringSchema().toJson()));
    ValidatorException exception = assertThrows(ValidatorException.class,
      () -> validator.validateParameter(param, new RequestParameterImpl("foo")));
    String expectedMsg =
      "Values in style " + style + " with exploded=false are not supported for header parameter dummy.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  private RequestBody mockRequestBody(boolean isRequired) {
    MediaType mockedMediaType = mock(MediaType.class);
    when(mockedMediaType.getSchema()).thenReturn(JsonSchema.of(objectSchema().toJson()));
    return mockRequestBody(isRequired, ImmutableMap.of(APPLICATION_JSON.toString(), mockedMediaType));
  }

  private RequestBody mockRequestBody(boolean isRequired, Map<String, MediaType> content) {
    RequestBody mockedRequestBody = mock(RequestBody.class);
    when(mockedRequestBody.isRequired()).thenReturn(isRequired);
    when(mockedRequestBody.getContent()).thenReturn(content);
    return mockedRequestBody;
  }

  @ParameterizedTest(name = "{index} If body is required, validateBody should throw an error if {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateBodyRequiredButNullOrEmpty(String scenario, RequestParameter parameter) {
    RequestBody mockedRequestBody = mockRequestBody(true);
    ValidatableRequest mockedValidatableRequest = mock(ValidatableRequest.class);
    when(mockedValidatableRequest.getBody()).thenReturn(parameter);

    ValidatorException exceptionEmpty =
      assertThrows(ValidatorException.class, () -> validator.validateBody(mockedRequestBody, mockedValidatableRequest));
    assertThat(exceptionEmpty.type()).isEqualTo(MISSING_REQUIRED_PARAMETER);
    String expectedMsg = "The related request does not contain the required body.";
    assertThat(exceptionEmpty).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} If body is not required, validateBody should return empty RequestParameter if {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateBodyNotRequiredAndBodyIsNullOrEmpty(String scenario, RequestParameter parameter) {
    RequestBody mockedRequestBody = mockRequestBody(false);
    ValidatableRequest mockedValidatableRequest = mock(ValidatableRequest.class);
    when(mockedValidatableRequest.getBody()).thenReturn(parameter);
    assertThat(validator.validateBody(mockedRequestBody, mockedValidatableRequest).isEmpty()).isTrue();
  }

  @ParameterizedTest(name = "validateBody should throw an error if MediaType or Transformer is null")
  @ValueSource(strings = {"text/plain", "foo/bar"})
  void testValidateBodyMediaTypeOrTransformerNull(String contentType) {
    RequestBody mockedRequestBody =
      mockRequestBody(false, ImmutableMap.of(TEXT_PLAIN.toString(), mock(MediaType.class)));

    ValidatableRequest mockedValidatableRequest = mock(ValidatableRequest.class);
    when(mockedValidatableRequest.getBody()).thenReturn(new RequestParameterImpl("foobar"));
    when(mockedValidatableRequest.getContentType()).thenReturn(contentType);

    ValidatorException exceptionEmpty =
      assertThrows(ValidatorException.class, () -> validator.validateBody(mockedRequestBody, mockedValidatableRequest));
    assertThat(exceptionEmpty.type()).isEqualTo(UNSUPPORTED_VALUE_FORMAT);
    String expectedMsg = "The format of the request body is not supported";
    assertThat(exceptionEmpty).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testValidateBodyThrowInvalidValue() {
    RequestBody mockedRequestBody = mockRequestBody(false);
    ValidatableRequest mockedValidatableRequest = mock(ValidatableRequest.class);
    when(mockedValidatableRequest.getBody()).thenReturn(new RequestParameterImpl(Buffer.buffer("3")));
    when(mockedValidatableRequest.getContentType()).thenReturn(APPLICATION_JSON.toString());

    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validateBody(mockedRequestBody, mockedValidatableRequest));
    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String reason = "Instance type number is invalid. Expected object";
    String expectedMsg =
      "The value of the request / response body is invalid. Reason: " + reason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
