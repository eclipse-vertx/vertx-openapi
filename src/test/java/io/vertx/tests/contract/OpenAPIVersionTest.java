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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.OpenAPIVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.contract.OpenAPIVersion.V3_0;
import static io.vertx.openapi.contract.OpenAPIVersion.V3_1;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.tests.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.tests.ResourceHelper.loadJson;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class OpenAPIVersionTest {
  private static final String DUMMY_BASE_URI = "app://";

  private static Stream<Arguments> provideVersionAndSpec() {
    return Stream.of(
      Arguments.of(V3_0, TEST_RESOURCE_PATH.resolve("v3.0").resolve("petstore.json")),
      Arguments.of(V3_1, TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json"))
    );
  }

  private static Stream<Arguments> provideVersionAndInvalidSpec() {
    Path basePath = getRelatedTestResourcePath(OpenAPIVersionTest.class);

    Function<String, Consumer<OutputUnit>> buildValidator = expectedString -> ou -> {
      String error = ou.getErrors().stream().map(OutputUnit::getError).collect(Collectors.joining());
      assertThat(error).contains(expectedString);
    };

    return Stream.of(
      Arguments.of(V3_0, basePath.resolve("v3_0_invalid_petstore.json"),
        buildValidator.apply("Property \"in\" does not match")),
      Arguments.of(V3_1, basePath.resolve("v3_1_invalid_petstore.json"),
        buildValidator.apply("Instance does not match any of [\"query\",\"header\",\"path\",\"cookie\"]")));
  }

  @ParameterizedTest(name = "{index} should validate a contract against OpenAPI version {0}")
  @MethodSource(value = "provideVersionAndSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void validateContractTest(OpenAPIVersion version, Path contractFile, Vertx vertx, VertxTestContext testContext) {
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> version.validateContract(vertx, repo, contract))
      .onComplete(testContext.succeeding(res -> {
        testContext.verify(() -> assertThat(res.getValid()).isTrue());
        testContext.completeNow();
      }));
  }

  @ParameterizedTest(name = "{index} should validate an invalid contract against OpenAPI version {0} and find errors")
  @MethodSource(value = "provideVersionAndInvalidSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void validateContractTestError(OpenAPIVersion version, Path contractFile, Consumer<OutputUnit> validator, Vertx vertx,
                                 VertxTestContext testContext) {
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> version.validateContract(vertx, repo, contract))
      .onComplete(testContext.succeeding(res -> {
        testContext.verify(() -> validator.accept(res));
        testContext.completeNow();
      }));
  }

  @ParameterizedTest(name = "{index} should resolve a contract of OpenAPI version {0}")
  @MethodSource(value = "provideVersionAndSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void testResolve(OpenAPIVersion version, Path contractFile, Vertx vertx, VertxTestContext testContext) {
    String dereferencedContractFile = contractFile.toString().replace(".json", "_dereferenced.json");
    JsonObject contractDereferenced = vertx.fileSystem().readFileBlocking(dereferencedContractFile).toJsonObject();
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();

    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> version.resolve(vertx, repo, contract))
      .onComplete(testContext.succeeding(res -> {
        testContext.verify(() -> assertThat(res).isEqualTo(contractDereferenced));
        testContext.completeNow();
      }));
  }

  @ParameterizedTest(name = "{index} should return a preloaded repository for OpenAPIVersion {0}")
  @EnumSource(OpenAPIVersion.class)
  @Timeout(value = 2, timeUnit = SECONDS)
  void testGetRepository(OpenAPIVersion version, Vertx vertx, VertxTestContext testContext) {
    version.getRepository(vertx, DUMMY_BASE_URI).onComplete(testContext.succeeding(repo -> testContext.verify(() -> {
      assertThat(repo).isInstanceOf(SchemaRepository.class);
      for (String ref : version.schemaFiles()) {
        assertThat(repo.find(ref)).isInstanceOf(JsonSchema.class);
      }
      testContext.completeNow();
    })));
  }

  @ParameterizedTest(name = "{index} test testFromSpec with OpenAPIVersion {0}")
  @MethodSource("provideVersionAndSpec")
  void testFromSpec(OpenAPIVersion version, Path specFile, Vertx vertx) {
    JsonObject spec = vertx.fileSystem().readFileBlocking(specFile.toString()).toJsonObject();
    assertThat(OpenAPIVersion.fromContract(spec)).isEqualTo(version);
  }

  @Test
  @DisplayName("fromSpec should throw exception if field openapi doesn't exist or the version isn't supported")
  void testFromSpecException() {
    String expectedInvalidMsg = "The passed OpenAPI contract is invalid: Field \"openapi\" is missing";
    Assertions.assertThrows(OpenAPIContractException.class, () -> OpenAPIVersion.fromContract(null),
      expectedInvalidMsg);
    assertThrows(OpenAPIContractException.class, () -> OpenAPIVersion.fromContract(EMPTY_JSON_OBJECT),
      expectedInvalidMsg);

    String expectedUnsupportedMsg = "The version of the passed OpenAPI contract is not supported: 2.0.0";
    JsonObject unsupportedContract = new JsonObject().put("openapi", "2.0.0");
    assertThrows(OpenAPIContractException.class, () -> OpenAPIVersion.fromContract(unsupportedContract),
      expectedUnsupportedMsg);
  }

  @ParameterizedTest(name = "{index} should be able to validate additional files against the json schema for {0}")
  @EnumSource(OpenAPIVersion.class)
  @Timeout(value = 2, timeUnit = SECONDS)
  public void testValidationOfAdditionalSchemaFiles(OpenAPIVersion version, Vertx vertx, VertxTestContext testContext) {
    Path path = getRelatedTestResourcePath(OpenAPIVersionTest.class).resolve("split");
    JsonObject validJsonSchema = loadJson(vertx, path.resolve("validJsonSchemaComponents.json"));
    JsonObject malformedJsonSchema = loadJson(vertx, path.resolve("malformedComponents.json"));


    version.getRepository(vertx, "https://vertx.io")
      .onSuccess(repository -> version.validateAdditionalContractFile(vertx, repository, validJsonSchema)
        .onFailure(testContext::failNow)
        .onSuccess(ignored -> version.validateAdditionalContractFile(vertx, repository, malformedJsonSchema)
          .onComplete(handler -> testContext.verify(() -> {
            assertThat(handler.failed()).isTrue();
            assertThat(handler.cause()).isInstanceOf(JsonSchemaValidationException.class);
            assertThat(handler.cause()).hasMessageThat().isEqualTo("-1 is less than 0");
            testContext.completeNow();
          }))));
  }

}
