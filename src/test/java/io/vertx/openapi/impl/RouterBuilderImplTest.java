package io.vertx.openapi.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.RouterBuilderException;
import io.vertx.openapi.Utils;
import io.vertx.openapi.objects.Operation;
import io.vertx.openapi.objects.impl.PathImpl;
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

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
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
      assertThat(PATHS_SORTED.get(x).getName()).isEqualTo(sorted.get(x).getName());
    }
  }

  @ParameterizedTest(name = "{index} applyMountOrder should throw error when {0}")
  @MethodSource
  void testApplyMountOrderThrows(String scenario, List<PathImpl> paths, String expectedReason) {
    RouterBuilderException exception =
      assertThrows(RouterBuilderException.class, () -> RouterBuilderImpl.applyMountOrder(paths));
    String expectedMsg = "The passed OpenAPI contract is invalid: " + expectedReason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testToVertxWebPath() {
    String openAPIPathWithPathParams = "/pets/{petId}/friends/{friendId}";
    String expectedWithPathParams = "/pets/:petId/friends/:friendId";
    assertThat(RouterBuilderImpl.toVertxWebPath(openAPIPathWithPathParams)).isEqualTo(expectedWithPathParams);

    String openAPIPathWithoutPathParams = "/pets/friends";
    assertThat(RouterBuilderImpl.toVertxWebPath(openAPIPathWithoutPathParams)).isEqualTo(openAPIPathWithoutPathParams);
  }

  @ParameterizedTest(name = "{index} should make operations of an OpenAPI ({0}) contract accessible")
  @ValueSource(strings = {"v3.0", "v3.1"})
  void testOperationAndOperations(String version) throws IOException {
    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve(version).resolve("petstore_dereferenced.json");
    JsonObject contract = Buffer.buffer(Files.readAllBytes(pathDereferencedContract)).toJsonObject();
    RouterBuilderImpl rb = new RouterBuilderImpl(contract, null);
    assertThat(rb.operations()).hasSize(3);

    Operation listPets = rb.operation("listPets");
    Operation createPets = rb.operation("createPets");
    Operation showPetById = rb.operation("showPetById");

    assertThat(listPets).isNotSameInstanceAs(createPets);
    assertThat(listPets).isNotSameInstanceAs(showPetById);
    assertThat(createPets).isNotSameInstanceAs(showPetById);
  }

  @Test
  void testRootHandler() {
    RouterBuilderImpl rb = new RouterBuilderImpl(new JsonObject(), null);
    assertThat(rb.rootHandlers).isEmpty();
    Handler<RoutingContext> dummyHandler = RoutingContext::next;
    rb.rootHandler(dummyHandler);
    assertThat(rb.rootHandlers).hasSize(1);
    assertThat(rb.rootHandlers.get(0)).isSameInstanceAs(dummyHandler);
  }
}
