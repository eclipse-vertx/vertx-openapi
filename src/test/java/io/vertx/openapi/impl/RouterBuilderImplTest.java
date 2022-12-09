package io.vertx.openapi.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.RouterBuilderException;
import io.vertx.openapi.Utils;
import io.vertx.openapi.objects.Operation;
import io.vertx.openapi.objects.impl.PathImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RouterBuilderImplTest {

  private static final List<PathImpl> PATHS_UNSORTED = Arrays.asList(
    new PathImpl("/v2", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/{foo}}/bar", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/docId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/pets/petId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/{docId}", Utils.EMPTY_JSON_OBJECT)
  );

  private static final List<PathImpl> PATHS_SORTED = Arrays.asList(
    new PathImpl("/pets/petId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/docId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v2", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/{docId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/{foo}}/bar", Utils.EMPTY_JSON_OBJECT)
  );

  private static Stream<Arguments> testApplyMountOrderThrows() {
    return Stream.of(
      Arguments.of("a duplicate has been found", Arrays.asList(
        new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
        new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT)
      ), "Found Path duplicate: /pets/{petId}"),
      Arguments.of("paths with same hierarchy but different templated names has been found", Arrays.asList(
        new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
        new PathImpl("/pets/{foo}", Utils.EMPTY_JSON_OBJECT)
      ), "Found Paths with same hierarchy but different templated names: /pets/{}")
    );
  }

  @RepeatedTest(10)
  void testApplyMountOrder() {
    List<PathImpl> shuffled = new ArrayList<>(PATHS_UNSORTED);
    Collections.shuffle(shuffled);
    List<PathImpl> sorted = RouterBuilderImpl.applyMountOrder(new ArrayList<>(PATHS_UNSORTED));
    for (int x = 0; x < PATHS_SORTED.size(); x++) {
      assertEquals(PATHS_SORTED.get(x).getName(), sorted.get(x).getName());
    }
  }

  @ParameterizedTest(name = "{index} applyMountOrder should throw error when {0}")
  @MethodSource
  void testApplyMountOrderThrows(String scenario, List<PathImpl> paths, String expectedReason) {
    RouterBuilderException exception =
      assertThrows(RouterBuilderException.class, () -> RouterBuilderImpl.applyMountOrder(paths));
    String expectedMsg = "The passed OpenAPI contract is invalid: " + expectedReason;
    assertEquals(expectedMsg, exception.getMessage());
  }

  @Test
  void testToVertxWebPath() {
    String openAPIPathWithPathParams = "/pets/{petId}/friends/{friendId}";
    String expectedWithPathParams = "/pets/:petId/friends/:friendId";
    assertEquals(expectedWithPathParams, RouterBuilderImpl.toVertxWebPath(openAPIPathWithPathParams));

    String openAPIPathWithoutPathParams = "/pets/friends";
    assertEquals(openAPIPathWithoutPathParams, RouterBuilderImpl.toVertxWebPath(openAPIPathWithoutPathParams));
  }

  @ParameterizedTest(name = "{index} should make operations of an OpenAPI ({0}) contract accessible")
  @ValueSource(strings = {"v3.0", "v3.1"})
  void testOperationAndOperations(String version) throws IOException {
    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve(version).resolve("petstore_dereferenced.json");
    JsonObject contract = Buffer.buffer(Files.readAllBytes(pathDereferencedContract)).toJsonObject();
    RouterBuilderImpl rb = new RouterBuilderImpl(contract, null);
    assertEquals(3, rb.operations().size());

    Operation listPets = rb.operation("listPets");
    Operation createPets = rb.operation("createPets");
    Operation showPetById = rb.operation("showPetById");

    Assertions.assertNotSame(listPets, createPets);
    Assertions.assertNotSame(listPets, showPetById);
    Assertions.assertNotSame(createPets, showPetById);
  }

  @Test
  void testRootHandler() {
    RouterBuilderImpl rb = new RouterBuilderImpl(new JsonObject(), null);
    assertEquals(0, rb.rootHandlers.size());
    Handler<RoutingContext> dummyHandler = RoutingContext::next;
    rb.rootHandler(dummyHandler);
    assertEquals(1, rb.rootHandlers.size());
    assertSame(dummyHandler, rb.rootHandlers.get(0));
  }
}
