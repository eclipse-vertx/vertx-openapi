import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.OpenAPIVersion;
import io.vertx.openapi.RouterBuilderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static io.vertx.openapi.OpenAPIVersion.V3_1;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class OpenAPIVersionTest {
  private static final Path RESOURCE_PATH = Paths.get("src", "test", "resources");
  private static final Path CONTRACT_FILE_V31 = RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");

  private static final String DUMMY_BASE_URI = "http://example.org";

  private static Stream<Arguments> provideVersionAndSpec() {
    return Stream.of(
      Arguments.of(V3_1, CONTRACT_FILE_V31)
    );
  }

  @ParameterizedTest
  @MethodSource(value = "provideVersionAndSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void testValidate(OpenAPIVersion version, Path contractFile, Vertx vertx, VertxTestContext testContext) {
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> {
      return V3_1.validate(vertx, repo, contract);
    }).onComplete(testContext.succeeding(res -> testContext.verify(() -> assertTrue(res.getValid()))));
  }

  @ParameterizedTest
  @MethodSource(value = "provideVersionAndSpec")
  @Timeout(value = 2, timeUnit = SECONDS)
  void testResolve(OpenAPIVersion version, Path contractFile, Vertx vertx, VertxTestContext testContext) {
    String dereferencedContractFile = contractFile.toString().replace(".json", "_dereferenced.json");
    JsonObject contractDereferenced = vertx.fileSystem().readFileBlocking(dereferencedContractFile).toJsonObject();
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();

    version.getRepository(vertx, DUMMY_BASE_URI).compose(repo -> V3_1.resolve(vertx, repo, contract))
      .onComplete(testContext.succeeding(res -> testContext.verify(() -> assertEquals(contractDereferenced, res))));
  }

  @Test
  @DisplayName("fromSpec should throw exception if field openapi doesn't exist")
  void testGetRepository(Vertx vertx, VertxTestContext testContext) {
    V3_1.getRepository(vertx, DUMMY_BASE_URI).onComplete(testContext.succeeding(repo -> {
      testContext.verify(() -> assertInstanceOf(SchemaRepository.class, repo));
    }));
  }

  @ParameterizedTest(name = "{index} test testFromSpec with OpenAPIVersion {0}")
  @MethodSource("provideVersionAndSpec")
  void testFromSpec(OpenAPIVersion version, Path specFile, Vertx vertx) {
    JsonObject spec = vertx.fileSystem().readFileBlocking(specFile.toString()).toJsonObject();
    Assertions.assertEquals(version, OpenAPIVersion.fromContract(spec));
  }

  @Test
  @DisplayName("fromSpec should throw exception if field openapi doesn't exist")
  void testFromSpecException() {
    JsonObject emptyContract = new JsonObject();
    assertThrows(RouterBuilderException.class, () -> OpenAPIVersion.fromContract(emptyContract));
  }
}
