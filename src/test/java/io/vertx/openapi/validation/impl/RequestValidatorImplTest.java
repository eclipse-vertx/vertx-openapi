package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestParameters;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.openapi.contract.Location.COOKIE;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class RequestValidatorImplTest {

  private RequestValidatorImpl validator;

  private OpenAPIContract contractSpy;

  private static Parameter buildParam(String name, JsonObject schema, boolean required) {
    return buildParam(name, PATH, schema, required);
  }

  private static Parameter buildParam(String name, Location in, JsonObject schema, boolean required) {
    return mockParameter(name, in, SIMPLE, false, JsonSchema.of(schema), required);
  }

  private static Stream<Arguments> provideNullRequestParameters() {
    return Stream.of(
      Arguments.of("RequestParameter is null", (Object) null),
      Arguments.of("Object in RequestParameter is null", new RequestParameterImpl(null))
    );
  }

  private static Stream<Arguments> testValidate() {
    List<Parameter> parameters = new ArrayList<>();
    parameters.add(buildParam("PathParamAge", PATH, intSchema().toJson(), true));
    parameters.add(buildParam("CookieParamAge", COOKIE, intSchema().toJson(), true));

    Map<String, RequestParameter> rawPathParameters = ImmutableMap.of("PathParamAge", new RequestParameterImpl("1337"));
    Map<String, RequestParameter> expectedPathParameters =
      ImmutableMap.of("PathParamAge", new RequestParameterImpl(1337));

    Map<String, RequestParameter> rawCookies = ImmutableMap.of("CookieParamAge", new RequestParameterImpl("9001"));
    Map<String, RequestParameter> expectedCookies =
      ImmutableMap.of("CookieParamAge", new RequestParameterImpl(9001));

    RequestParameters params = new RequestParametersImpl(rawCookies, null, rawPathParameters, null, null);
    RequestParameters expected = new RequestParametersImpl(expectedCookies, null, expectedPathParameters, null, null);

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
    })).onComplete(testContext.succeedingThenComplete());
  }

  @ParameterizedTest
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidate(List<Parameter> parameters, RequestParameters params, RequestParameters expected,
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
    })).onComplete(testContext.succeedingThenComplete());
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
}
