package io.vertx.openapi.validation.validator;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class PathParameterValidatorTest {

  private static final JsonSchema INT_SCHEMA = JsonSchema.of(intSchema().toJson());

  private PathParameterValidator validator;

  private static Parameter buildParam(String name, JsonObject schema, boolean required) {
    return mockParameter(name, PATH, SIMPLE, false, JsonSchema.of(schema), required);

  }

  private static Stream<Arguments> provideNullRequestParameters() {
    return Stream.of(
      Arguments.of("RequestParameter is null", (Object) null),
      Arguments.of("Object in RequestParameter is null", new RequestParameterImpl(null))
    );
  }

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract)
      .onSuccess(c -> this.validator = new PathParameterValidator(c.getSchemaRepository()))
      .onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void testValidate() {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    RequestParameter validated = validator.validate(param, new RequestParameterImpl("5"));
    assertThat(validated.getInteger()).isEqualTo(5);
  }

  @Test
  void testValidateThrowInvalidValue() {
    Parameter param = buildParam("p1", stringSchema().toJson(), false);
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validate(param, new RequestParameterImpl("3")));
    String expectedMsg =
      "The value of path parameter p1 is invalid. Reason: Instance type number is invalid. Expected string";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Throw error when required param is missing: {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateThrowRequired(String scenario, RequestParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), true);
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> validator.validate(param, value));
    String expectedMsg = "The related request does not contain the required path parameter p1";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Return RequestParameter with null, if parameter is missing and not required. {0}")
  @MethodSource("provideNullRequestParameters")
  void testValidateMissingParamIsNotRequired(String scenario, RequestParameter value) {
    Parameter param = buildParam("p1", intSchema().toJson(), false);
    assertThat(validator.validate(param, value).isEmpty()).isTrue();
  }
}
