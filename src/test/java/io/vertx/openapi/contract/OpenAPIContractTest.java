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

package io.vertx.openapi.contract;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.openapi.ResourceHelper.loadJson;

@ExtendWith(VertxExtension.class)
class OpenAPIContractTest {
  private static final Path BASE_PATH = getRelatedTestResourcePath(OpenAPIContractTest.class);

  private static Stream<Arguments> testFromWithPath() throws IOException {
    Path petstoreYaml = BASE_PATH.resolve("from_with_path/petstore.yaml");
    Path petstoreJson = BASE_PATH.resolve("from_with_path/petstore.json");
    Path dereferenced = BASE_PATH.resolve("from_with_path/petstore_dereferenced.json");
    JsonObject expectedJson = Buffer.buffer(Files.readAllBytes(dereferenced)).toJsonObject();
    return Stream.of(
      Arguments.of(petstoreYaml.toString(), expectedJson),
      Arguments.of(petstoreJson.toString(), expectedJson)
    );
  }

  private static Stream<Arguments> testFromWithPathAndAdditionalContractFiles() throws IOException {
    Path petstoreYaml = BASE_PATH.resolve("from_with_path_and_additional_files/petstore.yaml");
    Path petstoreJson = BASE_PATH.resolve("from_with_path_and_additional_files/petstore.json");
    Path componentsYaml = BASE_PATH.resolve("from_with_path_and_additional_files/components.yaml");
    Path componentsJson = BASE_PATH.resolve("from_with_path_and_additional_files/components.json");
    Path dereferenced = BASE_PATH.resolve("from_with_path_and_additional_files/bundled_dereferenced.json");
    String ref = "https://example.com/petstore";
    JsonObject expectedJson = Buffer.buffer(Files.readAllBytes(dereferenced)).toJsonObject();
    return Stream.of(
      Arguments.of(petstoreYaml.toString(), ImmutableMap.of(ref, componentsYaml.toString()), expectedJson),
      Arguments.of(petstoreJson.toString(), ImmutableMap.of(ref, componentsJson.toString()), expectedJson)
    );
  }

  @ParameterizedTest(name = "{index} Create contract from path: {0}")
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @MethodSource
  void testFromWithPath(String path, JsonObject expected, Vertx vertx, VertxTestContext testContext) {
    OpenAPIContract.from(vertx, path).onComplete(testContext.succeeding(contract -> testContext.verify(() -> {
      assertThat(contract.getRawContract()).isEqualTo(expected);
      testContext.completeNow();
    })));
  }

  @ParameterizedTest(name = "{index} Create contract from path with additional contract files: {0}")
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @MethodSource
  void testFromWithPathAndAdditionalContractFiles(String path, Map<String, String> additionalFiles, JsonObject expected,
    Vertx vertx, VertxTestContext testContext) {
    OpenAPIContract.from(vertx, path, additionalFiles)
      .onComplete(testContext.succeeding(contract -> testContext.verify(() -> {
        assertThat(contract.getRawContract()).isEqualTo(expected);
        testContext.completeNow();
      })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testFromFailsInvalidSpecMustNotNull(Vertx vertx, VertxTestContext testContext) {
    OpenAPIContract.from(vertx, (JsonObject) null).onComplete(testContext.failing(t -> testContext.verify(() -> {
      assertThat(t).isInstanceOf(OpenAPIContractException.class);
      assertThat(t).hasMessageThat().isEqualTo("The passed OpenAPI contract is invalid: Spec must not be null");
      testContext.completeNow();
    })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testFromFailsInvalidSpec(Vertx vertx, VertxTestContext testContext) {
    Path path = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("v3_0_invalid_petstore.json");
    JsonObject invalidContractJson = loadJson(vertx, path);

    OpenAPIContract.from(vertx, invalidContractJson).onComplete(testContext.failing(t -> testContext.verify(() -> {
      assertThat(t).isInstanceOf(OpenAPIContractException.class);
      assertThat(t).hasMessageThat().isEqualTo("The passed OpenAPI contract is invalid.");
      assertThat(t).hasCauseThat().isInstanceOf(JsonSchemaValidationException.class);
      testContext.completeNow();
    })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testInvalidAdditionalSpecFiles(Vertx vertx, VertxTestContext testContext) {
    Path resourcePath = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("split");
    JsonObject contract = loadJson(vertx, resourcePath.resolve("petstore.json"));
    JsonObject components = loadJson(vertx, resourcePath.resolve("invalidComponents.json"));
    Map<String, JsonObject> additionalSpecFiles = ImmutableMap.of("https://example.com/petstore", components);

    OpenAPIContract.from(vertx, contract, additionalSpecFiles)
      .onComplete(testContext.failing(t -> testContext.verify(() -> {
        assertThat(t).isInstanceOf(OpenAPIContractException.class);
        String expectedErrorMessage =
          "The passed OpenAPI contract is invalid: Found issue in specification for reference: https://example.com/petstore";
        assertThat(t).hasMessageThat().isEqualTo(expectedErrorMessage);
        testContext.completeNow();
      })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testSplitSpec(Vertx vertx, VertxTestContext testContext) {
    Path resourcePath = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("split");
    Path bundled = resourcePath.resolve("bundled_dereferenced.json");
    Path contractPath = resourcePath.resolve("petstore.json");
    Path componentsPath = resourcePath.resolve("components.json");

    JsonObject contract = loadJson(vertx, contractPath);
    JsonObject components = loadJson(vertx, componentsPath);

    Map<String, JsonObject> additionalSpecFiles =
      ImmutableMap.of("https://example.com/petstore", components);

    OpenAPIContract.from(vertx, contract, additionalSpecFiles).onComplete(testContext.succeeding(
      c -> testContext.verify(() -> {
        assertThat(c.getRawContract()).isEqualTo(loadJson(vertx, bundled));
        testContext.completeNow();
      })));
  }
}
