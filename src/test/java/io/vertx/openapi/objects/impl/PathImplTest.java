package io.vertx.openapi.objects.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.ResourceHelper;
import io.vertx.openapi.RouterBuilderException;
import io.vertx.openapi.objects.Operation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(VertxExtension.class)
class PathImplTest {
  private static final Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(PathImplTest.class);

  private static final Path VALID_PATHS_JSON = RESOURCE_PATH.resolve("path_valid.json");

  private static JsonObject validTestData;

  @BeforeAll
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_PATHS_JSON.toString()).toJsonObject();
  }

  private static PathImpl fromTestData(String id, JsonObject testData) {
    JsonObject testDataObject = testData.getJsonObject(id);
    String name = testDataObject.getString("name");
    JsonObject pathModel = testDataObject.getJsonObject("pathModel");
    return new PathImpl(name, pathModel);
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    PathImpl path = fromTestData(testId, validTestData);
    assertThat(path.getName()).isEqualTo("/pets/{petId}");
    assertThat(path.getParameters()).hasSize(1);

    assertThat(path.getOperations()).hasSize(1);
    Operation petById = path.getOperations().get(0);
    assertThat(petById).isNotNull();

    assertThat(petById.getOperationId()).isEqualTo("showPetById");
    assertThat(petById.getParameters()).hasSize(2);
  }

  @Test
  void testWilcardInPath() {
    RouterBuilderException exception =
      assertThrows(RouterBuilderException.class, () -> new PathImpl("/pets/*", EMPTY_JSON_OBJECT));
    String expectedMsg = "The passed OpenAPI contract is invalid: Paths must not have a wildcard (asterisk): /pets/*";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testCutTrailingSlash() {
    String expected = "/pets";
    assertThat(new PathImpl(expected + "/", EMPTY_JSON_OBJECT).getName()).isEqualTo(expected);
  }
}
