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

package io.vertx.tests.contract;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.OpenAPIContractException;
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
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.tests.ResourceHelper.loadJson;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
          "The passed OpenAPI contract is invalid: Found issue in specification for reference: " +
            "Can't resolve 'https://example.com/petstore#/components/schemas/Pet', only internal refs are supported.";
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

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testValidJsonSchemaProvidedAsAdditionalSpecFiles(Vertx vertx, VertxTestContext testContext) {
    Path resourcePath = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("split");
    JsonObject contract = loadJson(vertx, resourcePath.resolve("petstore.json"));
    JsonObject invalidComponents = loadJson(vertx, resourcePath.resolve("validJsonSchemaComponents.json"));
    JsonObject validComponents = loadJson(vertx, resourcePath.resolve("components.json"));

    Map<String, JsonObject> additionalValidSpecFiles = ImmutableMap.of("https://example.com/petstore", validComponents);
    Map<String, JsonObject> additionalInvalidSpecFiles = ImmutableMap.of("https://example.com/petstore",
      invalidComponents);

    OpenAPIContract.from(vertx, contract.copy(), additionalValidSpecFiles)
      .compose(validResp -> Future.succeededFuture(validResp.getRawContract()))
      .onSuccess(validJsonRef -> OpenAPIContract.from(vertx, contract.copy(), additionalInvalidSpecFiles)
        .onSuccess(splitResp -> testContext.verify(() -> {
          assertThat(splitResp.getRawContract()).isEqualTo(validJsonRef);
          testContext.completeNow();
        }))
        .onFailure(testContext::failNow))
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testMalformedJsonSchemaProvidedAsAdditionalSpecFiles(Vertx vertx, VertxTestContext testContext) {
    Path resourcePath = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("split");
    JsonObject contract = loadJson(vertx, resourcePath.resolve("petstore.json"));
    JsonObject malformedComponents = loadJson(vertx, resourcePath.resolve("malformedComponents.json"));

    Map<String, JsonObject> additionalMalformedSpecFiles = ImmutableMap.of("https://example.com/petstore",
      malformedComponents);

    OpenAPIContract.from(vertx, contract.copy(), additionalMalformedSpecFiles)
      .onComplete(handler -> testContext.verify(() -> {
        assertTrue(handler.failed());
        assertThat(handler.cause()).isInstanceOf(OpenAPIContractException.class);
        assertThat(handler.cause()).hasMessageThat()
          .isEqualTo("The passed OpenAPI contract is invalid: Failed to validate additional contract file: " +
            "https://example.com/petstore");
        assertThat(handler.cause().getCause()).hasMessageThat()
          .isEqualTo("-1 is less than 0");
        testContext.completeNow();
      }));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  public void testAdditionalSchemaFiles(Vertx vertx, VertxTestContext testContext) {
    Path resourcePath = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("additional_schema_files");
    Path contractPath = resourcePath.resolve("openapi.yaml");
    Path componentsPath = resourcePath.resolve("name.yaml");
    JsonObject dereferenced = loadJson(vertx, resourcePath.resolve("dereferenced.json"));

    Map<String, String> additionalSpecFiles = ImmutableMap.of("https://schemas/Name.yaml", componentsPath.toString());
    OpenAPIContract.from(vertx, contractPath.toString(), additionalSpecFiles)
      .onComplete(testContext.succeeding(c -> testContext.verify(() -> {
        assertThat(c.getRawContract().toString()).isEqualTo(dereferenced.toString());
        testContext.completeNow();
      })));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  public void testVendorSpecificJson(Vertx vertx, VertxTestContext testContext) {
    Path path = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("vendor_specific_json.json");
    JsonObject contractJson = loadJson(vertx, path);
    path = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("vendor_specific_json_dereferenced.json");
    JsonObject expectedJson = loadJson(vertx, path);

    OpenAPIContract.from(vertx, contractJson).onComplete(testContext.succeeding(c -> testContext.verify(() -> {
      assertThat(c.getRawContract().toString()).isEqualTo(expectedJson.toString());
      testContext.completeNow();
    })));
  }
}
