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

package io.vertx.tests.contract.impl;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.SchemaType.STRING;
import static io.vertx.openapi.contract.ContractErrorType.INVALID_SPEC;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_FEATURE;
import static io.vertx.openapi.contract.Location.COOKIE;
import static io.vertx.openapi.contract.Location.HEADER;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.contract.impl.ParameterImpl;
import io.vertx.tests.ResourceHelper;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
            "The passed OpenAPI contract is invalid: \"required\" MUST be true for path parameters"),
        Arguments.of("0003_With_Property_Content", UNSUPPORTED_FEATURE,
            "The passed OpenAPI contract contains a feature that is not supported: Usage of property \"content\" in parameter definition"),
        Arguments.of("0004_Without_Property_Content_And_Schema", INVALID_SPEC,
            "The passed OpenAPI contract is invalid: A parameter MUST contain either the \"schema\" or \"content\" property"),
        Arguments.of("0005_Path_With_Wrong_Style", INVALID_SPEC,
            "The passed OpenAPI contract is invalid: The style of a path parameter MUST be simple, label or matrix"),
        Arguments.of("0006_Cookie_With_Wrong_Style", INVALID_SPEC,
            "The passed OpenAPI contract is invalid: The style of a cookie parameter MUST be form"),
        Arguments.of("0007_Header_With_Wrong_Style", INVALID_SPEC,
            "The passed OpenAPI contract is invalid: The style of a header parameter MUST be simple"),
        Arguments.of("0008_Query_With_Wrong_Style", INVALID_SPEC,
            "The passed OpenAPI contract is invalid: The style of a query parameter MUST be form, spaceDelimited, pipeDelimited or deepObject"),
        Arguments.of("0009_Query_With_Unsupported_Style_DeepObject", UNSUPPORTED_FEATURE,
            "The passed OpenAPI contract contains a feature that is not supported: Query parameter in non-exploded deepObject style"),
        Arguments.of("0010_Query_With_Unsupported_Style_SpaceDelimited", UNSUPPORTED_FEATURE,
            "The passed OpenAPI contract contains a feature that is not supported: Parameters of style: spaceDelimited"),
        Arguments.of("0011_Query_With_Unsupported_Style_pipeDelimited", UNSUPPORTED_FEATURE,
            "The passed OpenAPI contract contains a feature that is not supported: Parameters of style: pipeDelimited"),
        Arguments.of("0012_With_Schema_No_Type", INVALID_SPEC,
            "The passed OpenAPI contract is invalid: Missing \"type\" for \"schema\" property in parameter: petId"),
        Arguments.of("0013_Cookie_With_Unsupported_Combination_Array_And_Exploded", UNSUPPORTED_FEATURE,
            "The passed OpenAPI contract contains a feature that is not supported: Cookie parameter values formatted as exploded array"));
  }

  private static Stream<Arguments> provideDefaultValuesScenarios() {
    return Stream.of(
        Arguments.of("0002_Default_Values_Cookie", COOKIE, FORM, true),
        Arguments.of("0003_Default_Values_Header", HEADER, SIMPLE, false),
        Arguments.of("0004_Default_Values_Path", PATH, SIMPLE, false),
        Arguments.of("0005_Default_Values_Query", QUERY, FORM, true));
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
    assertThat(param.getIn()).isEqualTo(HEADER);
    assertThat(param.isRequired()).isTrue();
    assertThat(param.getStyle()).isEqualTo(SIMPLE);
    assertThat(param.isExplode()).isTrue();
    assertThat(param.getSchema()).isEqualTo(JsonSchema.of(new JsonObject().put("type", "string")));
    assertThat(param.getSchemaType()).isEqualTo(STRING);

    JsonObject paramModel = validTestData.getJsonObject(testId).getJsonObject("parameterModel");
    assertThat(param.getOpenAPIModel()).isEqualTo(paramModel);
  }

  @Test
  void testPathParameter() {
    String testId = "0001_Path_Parameter";
    ParameterImpl param = fromTestData(testId, validTestData);
    assertThat(param.getName()).isEqualTo("petId");
    assertThat(param.getIn()).isEqualTo(PATH);
    assertThat(param.isRequired()).isTrue();
  }

  @ParameterizedTest(name = "{index} {1} should have style {2} and explode={3}")
  @MethodSource(value = "provideDefaultValuesScenarios")
  void testDefaultValues(String testId, Location in, Style expectedStyle, boolean isExploded) {
    ParameterImpl param = fromTestData(testId, validTestData);
    assertThat(param.getIn()).isEqualTo(in);
    assertThat(param.isRequired()).isEqualTo(in == PATH);
    assertThat(param.getStyle()).isEqualTo(expectedStyle);
    assertThat(param.isExplode()).isEqualTo(isExploded);
  }
}
