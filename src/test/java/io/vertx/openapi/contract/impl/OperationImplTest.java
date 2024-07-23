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

package io.vertx.openapi.contract.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.ResourceHelper;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.RequestBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.openapi.contract.ContractErrorType.INVALID_SPEC;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.impl.ParameterImpl.parseParameters;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class OperationImplTest {
  private static final Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(OperationImplTest.class);

  private static final Path VALID_OPERATIONS_JSON = RESOURCE_PATH.resolve("operation_valid.json");
  private static final Path INVALID_OPERATIONS_JSON = RESOURCE_PATH.resolve("operation_invalid.json");

  private static JsonObject validTestData;
  private static JsonObject invalidTestData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_OPERATIONS_JSON.toString()).toJsonObject();
    invalidTestData = vertx.fileSystem().readFileBlocking(INVALID_OPERATIONS_JSON.toString()).toJsonObject();
  }

  private static Stream<Arguments> provideErrorScenarios() {
    return Stream.of(
      Arguments.of("0000_Multiple_Exploded_Form_Parameters_In_Query_With_Content_Object", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: Found multiple exploded query parameters of style form with type " +
          "object in operation: showPetById"),
      Arguments.of("0001_No_Responses", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: No responses were found in operation: getPets"),
      Arguments.of("0002_Empty_Responses", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: No responses were found in operation: getPets")
    );
  }

  private static OperationImpl fromTestData(String id, JsonObject testData, SecurityRequirementImpl... secReqs) {
    JsonObject testDataObject = testData.getJsonObject(id);
    String path = testDataObject.getString("path");
    HttpMethod method = HttpMethod.valueOf(testDataObject.getString("method").toUpperCase());
    JsonObject operationModel = testDataObject.getJsonObject("operationModel");
    List<Parameter> pathParams = parseParameters(path, testDataObject.getJsonArray("pathParams", EMPTY_JSON_ARRAY));
    return new OperationImpl("/absolute" + path, path, method, operationModel, pathParams, emptyMap(),
      Arrays.asList(secReqs));
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
  void testParamFilter() {
    OperationImpl operation = fromTestData("0001_Filter_Path_Parameters", validTestData);

    List<Parameter> params = operation.getParameters();
    assertThat(params).hasSize(1);
    assertThat(params.get(0).isExplode()).isFalse();

    OperationImpl operation2 = fromTestData("0002_Do_Not_Filter_Path_Parameters", validTestData);

    assertThat(operation2.getParameters()).hasSize(2);
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    OperationImpl operation = fromTestData(testId, validTestData);
    assertThat(operation.getOperationId()).isEqualTo("showPetById");
    assertThat(operation.getOpenAPIPath()).isEqualTo("/pets/{petId}");
    assertThat(operation.getAbsoluteOpenAPIPath()).isEqualTo("/absolute/pets/{petId}");
    assertThat(operation.getHttpMethod()).isEqualTo(GET);
    assertThat(operation.getTags()).containsExactly("pets", "foo");
    assertThat(operation.getRequestBody()).isNull();
    assertThat(operation.getSecurityRequirements()).isEmpty();

    JsonObject operationModel = validTestData.getJsonObject(testId).getJsonObject("operationModel");
    assertThat(operation.getOpenAPIModel()).isEqualTo(operationModel);

    List<Parameter> params = operation.getParameters();
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getName()).isEqualTo("petId");
    assertThat(params.get(0).getIn()).isEqualTo(PATH);

    assertThat(operation.getDefaultResponse()).isNotNull();
    assertThat(operation.getResponse(200)).isNotNull();
  }

  @Test
  void testGetRequestBody() {
    String testId = "0003_Test_RequestBody";
    OperationImpl operation = fromTestData(testId, validTestData);
    assertThat(operation.getRequestBody()).isInstanceOf(RequestBody.class);
  }

  @Test
  void testGetSecurityRequirements() {
    SecurityRequirementImpl secReq = new SecurityRequirementImpl(new JsonObject().put("api_key", new JsonArray()));
    OperationImpl operation = fromTestData("0000_Test_Getters", validTestData, secReq);
    assertThat(operation.getSecurityRequirements()).hasSize(1);
    assertThat(operation.getSecurityRequirements().get(0).getNames()).containsExactly("api_key");

    OperationImpl operationWithSecReqs = fromTestData("0004_Test_Security_Requirements", validTestData);
    assertThat(operationWithSecReqs.getSecurityRequirements()).hasSize(1);
    assertThat(operationWithSecReqs.getSecurityRequirements().get(0).getNames()).containsExactly("api_key");
  }

  private static Stream<Arguments> providePathExtensions() {
    Map<String, Object> operationExtensions = ImmutableMap.of("x-some-string", "someString", "x-some-number", 1337);
    Arguments pathEmpty = Arguments.of("Path extensions are empty", emptyMap(), operationExtensions);

    Map<String, Object> pathWithSame = ImmutableMap.of("x-some-string", "pathValue");
    Arguments pathContainsSame = Arguments.of("Path contains an extensions with same name", pathWithSame,
      operationExtensions);

    Map<String, Object> pathWithNew = ImmutableMap.of("x-some-string", "pathValue", "x-path-extension",
      new JsonObject());
    Map<String, Object> expected = ImmutableMap.<String, Object>builder().putAll(operationExtensions).put("x-path" +
      "-extension", new JsonObject()).build();
    Arguments pathContainsAlsoNew = Arguments.of("Path contains an extensions with same name", pathWithNew, expected);

    return Stream.of(pathEmpty, pathContainsSame, pathContainsAlsoNew);
  }

  @ParameterizedTest(name = "{index} should provide the correct extensions: {0}")
  @MethodSource(value = "providePathExtensions")
  void testMergeExtensions(String scenario, Map<String, Object> pathExtensions, Map<String, Object> expected) {
    JsonObject testDataObject = validTestData.getJsonObject("0005_Test_Merge_Extensions");
    JsonObject operationModel = testDataObject.getJsonObject("operationModel");
    Operation op = new OperationImpl("/", "path", GET, operationModel, emptyList(), pathExtensions, emptyList());

    assertThat(op.getExtensions()).containsExactlyEntriesIn(expected);
  }
}
