package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.Utils;
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
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PATCH;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

  private static Stream<Arguments> testValidateWithOperationId() {
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
      this.contractSpy = Mockito.spy(c);
      this.validator = new RequestValidatorImpl(vertx, contractSpy);
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @ParameterizedTest
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateWithOperationId(List<Parameter> parameters, RequestParameters params, RequestParameters expected,
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
  void voidTestValidateWithPath(VertxTestContext testContext) {
    when(contractSpy.operation(anyString())).thenReturn(null);
    RequestValidator validatorSpy = spy(validator);
    validatorSpy.validate(null, "/pets", GET).onFailure(t -> testContext.verify(() -> {
      // A failure is expected here, due to the mocked contract. But it is only important
      // that validate(Request, operationId) was called
      verify(validatorSpy).validate(any(), eq("listPets"));
      testContext.completeNow();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateThrowOperationIdInValid(VertxTestContext testContext) {
    validator.validate(null, "invalidId").onFailure(t -> testContext.verify(() -> {
      assertThat(t).hasMessageThat().isEqualTo("Invalid OperationId: invalidId");
      testContext.completeNow();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidateThrowOperationNotFound(VertxTestContext testContext) {
    Checkpoint cp = testContext.checkpoint(2);
    validator.validate(null, "/path/dont/exist", PATCH).onFailure(t -> testContext.verify(() -> {
      assertThat(t).hasMessageThat().isEqualTo("No operation found for the request: PATCH /path/dont/exist");
      cp.flag();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));

    validator.validate(null, "/pets", PATCH).onFailure(t -> testContext.verify(() -> {
      assertThat(t).hasMessageThat().isEqualTo("No operation found for the request: PATCH /pets");
      cp.flag();
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
    String expectedMsg =
      "The value of path parameter p1 is invalid. Reason: Instance type number is invalid. Expected string";
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
    Parameter param = mockParameter("dummy", HEADER, style, false, JsonSchema.of(Utils.EMPTY_JSON_OBJECT));
    ValidatorException exception = assertThrows(ValidatorException.class,
      () -> validator.validateParameter(param, new RequestParameterImpl("foo")));
    String expectedMsg =
      "Values in style " + style + " with exploded=false are not supported for header parameter dummy.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
