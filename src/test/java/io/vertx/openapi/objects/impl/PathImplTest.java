package io.vertx.openapi.objects.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.ResourceHelper;
import io.vertx.openapi.objects.Operation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class PathImplTest {
  private static Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(PathImplTest.class);

  private static Path VALID_PATHS_JSON = RESOURCE_PATH.resolve("path_valid.json");

  private static JsonObject validTestData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx, VertxTestContext testContext) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_PATHS_JSON.toString()).toJsonObject();
    testContext.completeNow();
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
    assertEquals("/pets/{petId}", path.getName());

    assertEquals(1, path.getOperations().size());
    Operation petById = path.getOperations().get(0);
    assertNotNull(petById);
    assertEquals("showPetById", petById.getOperationId());
    assertEquals(2, petById.getParameters().size());

    assertEquals(1, path.getParameters().size());
  }
}
