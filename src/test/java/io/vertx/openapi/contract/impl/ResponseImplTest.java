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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.OpenAPIContractException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.json.schema.common.dsl.SchemaType.INTEGER;
import static io.vertx.json.schema.common.dsl.SchemaType.STRING;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_FEATURE;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class ResponseImplTest {
  private static final Path RESOURCE_PATH = getRelatedTestResourcePath(ResponseImplTest.class);
  private static final Path VALID_RESPONSE_JSON = RESOURCE_PATH.resolve("response_valid.json");
  private static final Path INVALID_RESPONSE_JSON = RESOURCE_PATH.resolve("response_invalid.json");

  private static final String DUMMY_OPERATION_ID = "dummyOperation";

  private static JsonObject validTestData;

  private static JsonObject invalidTestData;

  @BeforeAll
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_RESPONSE_JSON.toString()).toJsonObject();
    invalidTestData = vertx.fileSystem().readFileBlocking(INVALID_RESPONSE_JSON.toString()).toJsonObject();
  }

  private static Stream<Arguments> testGetters() {
    String contentKey = APPLICATION_JSON.toString();
    return Stream.of(
      Arguments.of("0000_Test_Getters_No_Headers", 1, contentKey, 0, null),
      Arguments.of("0001_Test_Getters_With_Headers", 1, contentKey, 1, STRING),
      Arguments.of("0002_Test_Getters_With_Headers_Ignore_Content_Type_Header", 1, contentKey, 1, INTEGER),
      Arguments.of("0003_Test_Getters_No_Headers_No_Content", 0, null, 0, null)
    );
  }

  private static Stream<Arguments> testExceptions() {
    return Stream.of(
      Arguments.of("0000_Response_With_Content_Type_Text_Plain", UNSUPPORTED_FEATURE,
        "The passed OpenAPI contract contains a feature that is not supported: Operation dummyOperation defines a " +
          "response with an unsupported media type. Supported: application/json, application/json; charset=utf-8, " +
          "multipart/form-data")
    );
  }

  @ParameterizedTest(name = "{index} test getters for scenario: {0}")
  @MethodSource
  void testGetters(String testId, int contentSize, String contentKey, int headerSize, SchemaType type) {
    JsonObject responseModel = validTestData.getJsonObject(testId);
    ResponseImpl response = new ResponseImpl(responseModel, DUMMY_OPERATION_ID);

    assertThat(response.getHeaders()).hasSize(headerSize);
    if (headerSize > 0) {
      SchemaType actualType = response.getHeaders().stream().findFirst().get().getSchemaType();
      assertThat(actualType).isEqualTo(type);
    }

    assertThat(response.getOpenAPIModel()).isEqualTo(responseModel);
    assertThat(response.getContent()).hasSize(contentSize);
    if (contentSize > 0) {
      assertThat(response.getContent()).containsKey(contentKey);
    }
  }

  @ParameterizedTest(name = "{index} should throw an exception for scenario: {0}")
  @MethodSource
  void testExceptions(String testId, ContractErrorType type, String msg) {
    JsonObject response = invalidTestData.getJsonObject(testId);
    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> new ResponseImpl(response, DUMMY_OPERATION_ID));
    assertThat(exception.type()).isEqualTo(type);
    assertThat(exception).hasMessageThat().isEqualTo(msg);
  }
}
