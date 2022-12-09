package io.vertx.openapi.objects.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.ErrorType;
import io.vertx.openapi.ResourceHelper;
import io.vertx.openapi.RouterBuilderException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static io.vertx.openapi.ErrorType.INVALID_SPEC;
import static io.vertx.openapi.objects.Location.PATH;
import static io.vertx.openapi.objects.Location.QUERY;
import static io.vertx.openapi.objects.Style.FORM;
import static io.vertx.openapi.objects.Style.MATRIX;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class ParameterImplTest {

  private static Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(ParameterImplTest.class);

  private static Path VALID_PARAMETERS_JSON = RESOURCE_PATH.resolve("parameter_valid.json");

  private static Path INVALID_PARAMETERS_JSON = RESOURCE_PATH.resolve("parameter_invalid.json");

  private static JsonObject validTestData;

  private static JsonObject invalidTestData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx, VertxTestContext testContext) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_PARAMETERS_JSON.toString()).toJsonObject();
    invalidTestData = vertx.fileSystem().readFileBlocking(INVALID_PARAMETERS_JSON.toString()).toJsonObject();
    testContext.completeNow();
  }

  private static ParameterImpl fromTestData(String id, JsonObject testData) {
    JsonObject testDataObject = testData.getJsonObject(id);
    String path = testDataObject.getString("path");
    JsonObject parameterModel = testDataObject.getJsonObject("parameterModel");
    return new ParameterImpl(path, parameterModel);
  }

  private static Stream<Arguments> provideErrorScenarios() {
    return Stream.of(
      Arguments.of("0000_Path_With_No_Name", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: Path parameters MUST have a name that is part of the path"),
      Arguments.of("0001_Path_With_Empty_Name", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: Path parameters MUST have a name that is part of the path"),
      Arguments.of("0002_Path_Without_Require", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: \"required\" MUST be true for path parameters")
    );
  }

  @ParameterizedTest(name = "{index} should throw an exception for scenario: {0}")
  @MethodSource(value = "provideErrorScenarios")
  void testExceptions(String testId, ErrorType type, String msg) {
    RouterBuilderException exception =
      assertThrows(RouterBuilderException.class, () -> fromTestData(testId, invalidTestData));
    assertEquals(type, exception.type());
    assertEquals(msg, exception.getMessage());
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertEquals("petId", param.getName());
    assertEquals(QUERY, param.getIn());
    assertEquals(true, param.isRequired());
    assertEquals(MATRIX, param.getStyle());
    assertEquals(true, param.isExplode());
    JsonObject paramModel = validTestData.getJsonObject(testId).getJsonObject("parameterModel");
    assertEquals(paramModel, param.getParameterModel());
  }

  @Test
  void testPathParameter() {
    String testId = "0001_Path_Parameter";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertEquals("petId", param.getName());
    assertEquals(PATH, param.getIn());
    assertEquals(true, param.isRequired());
  }

  @Test
  void testDefaultValues() {
    String testId = "0002_Default_Values";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertEquals(false, param.isRequired());
    assertEquals(FORM, param.getStyle());
    assertEquals(false, param.isExplode());
  }
}
