package io.vertx.openapi.contract.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.ResourceHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.contract.ContractErrorType.INVALID_SPEC;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.MATRIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class ParameterImplTest {

  private static final Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(ParameterImplTest.class);

  private static final Path VALID_PARAMETERS_JSON = RESOURCE_PATH.resolve("parameter_valid.json");

  private static final Path INVALID_PARAMETERS_JSON = RESOURCE_PATH.resolve("parameter_invalid.json");

  private static JsonObject validTestData;

  private static JsonObject invalidTestData;

  @BeforeAll
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_PARAMETERS_JSON.toString()).toJsonObject();
    invalidTestData = vertx.fileSystem().readFileBlocking(INVALID_PARAMETERS_JSON.toString()).toJsonObject();
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
  void testExceptions(String testId, ContractErrorType type, String msg) {
    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> fromTestData(testId, invalidTestData));
    assertThat(exception.type()).isEqualTo(type);
    assertThat(exception).hasMessageThat().isEqualTo(msg);
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertThat(param.getName()).isEqualTo("petId");
    assertThat(param.getIn()).isEqualTo(QUERY);
    assertThat(param.isRequired()).isTrue();
    assertThat(param.getStyle()).isEqualTo(MATRIX);
    assertThat(param.isExplode()).isTrue();

    JsonObject paramModel = validTestData.getJsonObject(testId).getJsonObject("parameterModel");
    assertThat(param.getParameterModel()).isEqualTo(paramModel);
  }

  @Test
  void testPathParameter() {
    String testId = "0001_Path_Parameter";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertThat(param.getName()).isEqualTo("petId");
    assertThat(param.getIn()).isEqualTo(PATH);
    assertThat(param.isRequired()).isTrue();
  }

  @Test
  void testDefaultValues() {
    String testId = "0002_Default_Values";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertThat(param.isRequired()).isFalse();
    assertEquals(FORM, param.getStyle());
    assertThat(param.isExplode()).isFalse();
  }
}
