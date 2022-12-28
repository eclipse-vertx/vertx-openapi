package io.vertx.openapi;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
import static io.vertx.openapi.OpenAPIVersion.V3_0;
import static io.vertx.openapi.OpenAPIVersion.V3_1;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
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
    Path basePath = ResourceHelper.getRelatedTestResourcePath(OpenAPIVersionTest.class);

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
  void testValidate(OpenAPIVersion version, Path contractFile, Vertx vertx, VertxTestContext testContext) {
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> version.validate(vertx, repo, contract))
      .onComplete(testContext.succeeding(res -> {
        testContext.verify(() -> assertThat(res.getValid()).isTrue());
        testContext.completeNow();
      }));
  }

  @ParameterizedTest(name = "{index} should validate an invalid contract against OpenAPI version {0} and find errors")
  @MethodSource(value = "provideVersionAndInvalidSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void testValidateError(OpenAPIVersion version, Path contractFile, Consumer<OutputUnit> validator, Vertx vertx,
    VertxTestContext testContext) {
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> version.validate(vertx, repo, contract))
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
      for (String ref : version.schemaFiles) {
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
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(null), expectedInvalidMsg);
    JsonObject emptyContract = new JsonObject();
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(emptyContract), expectedInvalidMsg);

    String expectedUnsupportedMsg = "The version of the passed OpenAPI contract is not supported: 2.0.0";
    JsonObject unsupportedContract = new JsonObject().put("openapi", "2.0.0");
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(unsupportedContract),
      expectedUnsupportedMsg);
  }
}
