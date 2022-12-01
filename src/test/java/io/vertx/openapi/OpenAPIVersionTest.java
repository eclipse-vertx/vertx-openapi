package io.vertx.openapi;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static io.vertx.openapi.OpenAPIVersion.V3_0;
import static io.vertx.openapi.OpenAPIVersion.V3_1;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class OpenAPIVersionTest {
  private static final Path CONTRACT_FILE_V30 = TEST_RESOURCE_PATH.resolve("v3.0").resolve("petstore.json");
  private static final Path CONTRACT_FILE_V31 = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");

  private static final String DUMMY_BASE_URI = "app://";

  private static Stream<Arguments> provideVersionAndSpec() {
    return Stream.of(
      Arguments.of(V3_0, CONTRACT_FILE_V30),
      Arguments.of(V3_1, CONTRACT_FILE_V31)
    );
  }

  @ParameterizedTest(name = "{index} should validate a contract against OpenAPI version {0}")
  @MethodSource(value = "provideVersionAndSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void testValidate(OpenAPIVersion version, Path contractFile, Vertx vertx, VertxTestContext testContext) {
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> {
      return version.validate(vertx, repo, contract);
    }).onComplete(testContext.succeeding(res -> {
      testContext.verify(() -> assertTrue(res.getValid()));
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
        testContext.verify(() -> assertEquals(contractDereferenced, res));
        testContext.completeNow();
      }));
  }

  @ParameterizedTest(name = "{index} should return a preloaded repository for OpenAPIVersion {0}")
  @EnumSource(OpenAPIVersion.class)
  @Timeout(value = 2, timeUnit = SECONDS)
  void testGetRepository(OpenAPIVersion version, Vertx vertx, VertxTestContext testContext) {
    version.getRepository(vertx, DUMMY_BASE_URI).onComplete(testContext.succeeding(repo -> testContext.verify(() -> {
      assertInstanceOf(SchemaRepository.class, repo);
      for (String ref : version.schemaFiles) {
        assertInstanceOf(JsonSchema.class, repo.find(ref));
      }
      testContext.completeNow();
    })));
  }

  @ParameterizedTest(name = "{index} test testFromSpec with OpenAPIVersion {0}")
  @MethodSource("provideVersionAndSpec")
  void testFromSpec(OpenAPIVersion version, Path specFile, Vertx vertx) {
    JsonObject spec = vertx.fileSystem().readFileBlocking(specFile.toString()).toJsonObject();
    Assertions.assertEquals(version, OpenAPIVersion.fromContract(spec));
  }

  @Test
  @DisplayName("fromSpec should throw exception if field openapi doesn't exist or the version isn't supported")
  void testFromSpecException() {
    String expectedInvalidMsg = "The passed OpenAPI contract is invalid: Field \"openapi\" is missing";
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(null), expectedInvalidMsg);
    JsonObject emptyContract = new JsonObject();
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(emptyContract), expectedInvalidMsg);

    String expectedUnsupportedMsg = "The version of the passed OpenAPI contract is not supported: 2.0.0";
    JsonObject unsupportedContract = new JsonObject().put("openapi", "2.0.0");
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(unsupportedContract),
      expectedUnsupportedMsg);
  }
}
