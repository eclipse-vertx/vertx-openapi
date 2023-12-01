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
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.RequestBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.openapi.contract.ContractErrorType.INVALID_SPEC;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_FEATURE;
import static io.vertx.openapi.contract.MediaType.APPLICATION_JSON;
import static io.vertx.openapi.contract.MediaType.APPLICATION_JSON_UTF8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class RequestBodyImplTest {

  private static final Path RESOURCE_PATH = getRelatedTestResourcePath(RequestBodyImplTest.class);

  private static final Path VALID_REQUEST_BODY_JSON = RESOURCE_PATH.resolve("requestBody_valid.json");

  private static final Path INVALID_REQUEST_BODY_JSON = RESOURCE_PATH.resolve("requestBody_invalid.json");

  private static final String DUMMY_OPERATION_ID = "dummyOperation";

  private static JsonObject validTestData;

  private static JsonObject invalidTestData;

  @BeforeAll
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_REQUEST_BODY_JSON.toString()).toJsonObject();
    invalidTestData = vertx.fileSystem().readFileBlocking(INVALID_REQUEST_BODY_JSON.toString()).toJsonObject();
  }

  private static Stream<Arguments> testGetters() {
    return Stream.of(
      Arguments.of("0000_Test_Getters_Required_True_Content_Schema_String", true),
      Arguments.of("0001_Test_Getters_Required_False_Content_Schema_String", false),
      Arguments.of("0002_Test_Getters_Without_Required_Content_Schema_String", false)
    );
  }

  private static Stream<Arguments> testExceptions() {
    return Stream.of(
      Arguments.of("0000_RequestBody_Without_Content", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: Operation dummyOperation defines a request body without or with " +
          "empty property \"content\""),
      Arguments.of("0001_RequestBody_With_Empty_Content", INVALID_SPEC,
        "The passed OpenAPI contract is invalid: Operation dummyOperation defines a request body without or with " +
          "empty property \"content\""),
      Arguments.of("0002_RequestBody_With_Content_Type_Text_Plain", UNSUPPORTED_FEATURE,
        "The passed OpenAPI contract contains a feature that is not supported: Operation dummyOperation defines a " +
          "request body with an unsupported media type. Supported: application/json, application/json; charset=utf-8")
    );
  }

  @ParameterizedTest(name = "{index} test getters for scenario: {0}")
  @MethodSource
  void testGetters(String testId, boolean required) {
    JsonObject requestBodyModel = validTestData.getJsonObject(testId);
    RequestBodyImpl requestBody = new RequestBodyImpl(requestBodyModel, DUMMY_OPERATION_ID);

    assertThat(requestBody.isRequired()).isEqualTo(required);
    assertThat(requestBody.getOpenAPIModel()).isEqualTo(requestBodyModel);
    assertThat(requestBody.getContent()).hasSize(1);
    assertThat(requestBody.getContent()).containsKey(APPLICATION_JSON);
  }

  @ParameterizedTest(name = "{index} should throw an exception for scenario: {0}")
  @MethodSource
  void testExceptions(String testId, ContractErrorType type, String msg) {
    JsonObject requestBody = invalidTestData.getJsonObject(testId);
    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> new RequestBodyImpl(requestBody, DUMMY_OPERATION_ID));
    assertThat(exception.type()).isEqualTo(type);
    assertThat(exception).hasMessageThat().isEqualTo(msg);
  }


  private RequestBodyImpl buildWithContent(String... contentTypes) {
    JsonObject dummySchema = new JsonObject().put("schema", new JsonObject().put("type", "string"));
    JsonObject content = new JsonObject();
    for (String type : contentTypes) {
      content.put(type, dummySchema);
    }
    return new RequestBodyImpl(new JsonObject().put("content", content), DUMMY_OPERATION_ID);
  }

  @Test
  void testDetermineContentType() {
    String appJson = APPLICATION_JSON;
    String appJsonUTF8 = APPLICATION_JSON_UTF8;
    RequestBody bodyBoth = buildWithContent(appJson, appJsonUTF8);
    RequestBody bodyAppJson = buildWithContent(appJson);
    RequestBody bodyAppJsonUTF8 = buildWithContent(appJsonUTF8);

    assertThat(bodyBoth.determineContentType(null)).isNull();
    assertThat(bodyBoth.determineContentType(appJson).getIdentifier()).isEqualTo(appJson);
    assertThat(bodyBoth.determineContentType(appJsonUTF8).getIdentifier()).isEqualTo(appJsonUTF8);

    assertThat(bodyAppJson.determineContentType(appJsonUTF8).getIdentifier()).isEqualTo(appJson);
    assertThat(bodyAppJsonUTF8.determineContentType(appJson)).isNull();

    // No Whitespace before semicolon
    assertThat(bodyBoth.determineContentType(appJson + ";charset=utf-8").getIdentifier()).isEqualTo(APPLICATION_JSON_UTF8);

    assertThat(bodyBoth.determineContentType("application/text")).isNull();
  }
}
