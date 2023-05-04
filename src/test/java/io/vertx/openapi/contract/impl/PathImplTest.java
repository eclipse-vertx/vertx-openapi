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
import io.vertx.openapi.ResourceHelper;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Operation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.contract.impl.PathImpl.INVALID_CURLY_BRACES;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class PathImplTest {
  private static final Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(PathImplTest.class);

  private static final Path VALID_PATHS_JSON = RESOURCE_PATH.resolve("path_valid.json");
  private static final String BASE_PATH = "";
  private static JsonObject validTestData;

  @BeforeAll
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_PATHS_JSON.toString()).toJsonObject();
  }

  private static PathImpl fromTestData(String id, JsonObject testData) {
    JsonObject testDataObject = testData.getJsonObject(id);
    String name = testDataObject.getString("name");
    JsonObject pathModel = testDataObject.getJsonObject("pathModel");
    return new PathImpl(BASE_PATH, name, pathModel, emptyList());
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    PathImpl path = fromTestData(testId, validTestData);
    assertThat(path.getName()).isEqualTo("/pets/{petId}");
    assertThat(path.toString()).isEqualTo("/pets/{petId}");
    assertThat(path.getParameters()).hasSize(1);

    assertThat(path.getOperations()).hasSize(1);
    Operation petById = path.getOperations().get(0);
    assertThat(petById).isNotNull();

    assertThat(petById.getOperationId()).isEqualTo("showPetById");
    assertThat(petById.getParameters()).hasSize(2);

    assertThat(path.getOpenAPIModel().getJsonArray("parameters")).hasSize(1);
  }

  @Test
  void testWildcardInPath() {
    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> new PathImpl(BASE_PATH, "/pets/*", EMPTY_JSON_OBJECT,
        emptyList()));
    String expectedMsg = "The passed OpenAPI contract is invalid: Paths must not have a wildcard (asterisk): /pets/*";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} wrong position of curley braces in path: {0}")
  @ValueSource(strings = {"/foo{param}/", "/foo{param}", "/{param}bar/", "/{param}bar", "/foo{param}bar/",
    "/foo{param}bar"})
  void testWrongCurlyBracesInPath(String path) {
    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> new PathImpl(BASE_PATH, path, EMPTY_JSON_OBJECT, emptyList()));
    String expectedMsg =
      "The passed OpenAPI contract is invalid: Curly brace MUST be the first/last character in a path segment " +
        "(/{parameterName}/): " + path;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} valid position of curley braces in path: {0}")
  @ValueSource(strings = {"/foo/{param}", "/foo/{param}/", "/foo/{param}/bar", "/foo/{param}/bar/"})
  void testValidCurlyBracesInPath(String path) {
    assertThat(INVALID_CURLY_BRACES.matcher(path).find()).isFalse();
  }

  @Test
  void testCutTrailingSlash() {
    String expected = "/pets";
    assertThat(new PathImpl(BASE_PATH, expected + "/", EMPTY_JSON_OBJECT, emptyList()).getName()).isEqualTo(expected);
  }

  @Test
  void testRootPath() {
    String expected = "/";
    assertThat(new PathImpl(BASE_PATH, expected, EMPTY_JSON_OBJECT, emptyList()).getName()).isEqualTo(expected);
  }

  @Test
  void testGetAbsolutePath() {
    String expected = "/base/foo";
    assertThat(new PathImpl("/base", "/foo", EMPTY_JSON_OBJECT, emptyList()).getAbsolutePath()).isEqualTo(expected);
    assertThat(new PathImpl("/base/", "/foo", EMPTY_JSON_OBJECT, emptyList()).getAbsolutePath()).isEqualTo(expected);
  }
}
