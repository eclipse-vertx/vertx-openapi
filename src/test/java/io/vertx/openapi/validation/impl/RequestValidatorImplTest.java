package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.OutputUnit;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestParameters;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
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

  private static Parameter buildParam(String name, Location in, Style style, JsonObject schema, boolean required) {
    return mockParameter(name, in, style, false, JsonSchema.of(schema), required);
  }

  private static Stream<Arguments> provideNullRequestParameters() {
    return Stream.of(
      Arguments.of("RequestParameter is null", null),
      Arguments.of("Object in RequestParameter is null", new RequestParameterImpl(null))
    );
  }

  private static Stream<Arguments> testValidateWithParamsAndOperationId() {
    List<Parameter> parameters = new ArrayList<>();
    parameters.add(buildParam("CookieParamAge", COOKIE, FORM, intSchema().toJson(), true));
    parameters.add(buildParam("HeaderParamUser", HEADER, SIMPLE, objectSchema().toJson(), true));
    parameters.add(buildParam("PathParamVersion", PATH, SIMPLE, numberSchema().toJson(), true));
    parameters.add(buildParam("QueryParamTrace", QUERY, FORM, booleanSchema().toJson(), true));

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

    RequestParameters expected =
      new RequestParametersImpl(expectedCookies, expectedHeaderParameters, expectedPathParameters, expectedQuery, null);
    RequestParameters params =
      new RequestParametersImpl(rawCookies, rawHeaderParameters, rawPathParameters, rawQuery, null);

    return Stream.of(
      Arguments.of(parameters, params, expected)
    );
  }

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract).onSuccess(c -> testContext.verify(() -> {
      this.contractSpy = spy(c);
      this.validator = new RequestValidatorImpl(vertx, contractSpy);
      testContext.completeNow();
    })).onFailure(testContext::failNow);
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
      verify(validatorSpy).validate(isA(RequestParameters.class), eq(operationId));
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
      verify(validatorSpy).validate(isA(RequestParameters.class), eq("isMocked"));
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @ParameterizedTest
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateWithParamsAndOperationId(List<Parameter> parameters, RequestParameters params,
    RequestParameters expected,
    VertxTestContext testContext) {
    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getParameters()).thenReturn(parameters);
    when(contractSpy.operation(anyString())).thenReturn(mockedOperation);

    validator.validate(params, "isMocked").onSuccess(validatedParams -> testContext.verify(() -> {
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

    validator.validate((RequestParameters) null, "invalidId").onFailure(t -> testContext.verify(() -> {
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

    RequestParameters params =
      new RequestParametersImpl(null, rawHeaderParameters, null, null, null);

    Operation mockedOperation = mock(Operation.class);
    when(mockedOperation.getParameters()).thenReturn(parameters);
    when(contractSpy.operation(anyString())).thenReturn(mockedOperation);

    String expected = "The formatting of the value of header parameter HeaderParamUser doesn't match to style simple.";
    validator.validate(params, "isMocked").onFailure(e -> testContext.verify(() -> {
      assertThat(e).hasMessageThat().isEqualTo(expected);
      cp.flag();
    })).onComplete(testContext.failing(validatedParams -> cp.flag()));
  }

  private Parameter buildParam(String name, JsonObject schema, boolean required) {
    return buildParam(name, PATH, SIMPLE, schema, required);
  }

  @Test
  void testValidateParameter() {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    RequestParameter validated = validator.validateParameter(param, new RequestParameterImpl("5"));
    assertThat(validated.getInteger()).isEqualTo(5);
  }

  @Test
  void testValidateParameterThrowInvalidValue() {
    Parameter param = buildParam("p1", stringSchema().toJson(), false);
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validateParameter(param, new RequestParameterImpl("3")));
    assertThat(exception.type()).isEqualTo(INVALID_VALUE);
    String reason = "Instance type number is invalid. Expected string";
    assertThat(exception.getReason()).isInstanceOf(OutputUnit.class);
    String expectedMsg =
      "The value of path parameter p1 is invalid. Reason: " + reason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Throw error when required param is missing: {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateParameterThrowRequired(String scenario, RequestParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validateParameter(param, value));
    String expectedMsg = "The related request does not contain the required path parameter p1";
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
}
