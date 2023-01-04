package io.vertx.openapi.router.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.contract.OpenAPIVersion;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.openapi.contract.OpenAPIVersion.V3_1;

class RouterBuilderImplTest {

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
    RouterBuilderImpl rb =
      new RouterBuilderImpl(null, new OpenAPIContractImpl(contract, OpenAPIVersion.fromContract(contract)));
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
    JsonObject dummySpec = new JsonObject().put("openapi", "3.0.0");
    RouterBuilderImpl rb = new RouterBuilderImpl(null, new OpenAPIContractImpl(dummySpec, V3_1));
    assertThat(rb.rootHandlers).isEmpty();
    Handler<RoutingContext> dummyHandler = RoutingContext::next;
    rb.rootHandler(dummyHandler);
    assertThat(rb.rootHandlers).hasSize(1);
    assertThat(rb.rootHandlers.get(0)).isSameInstanceAs(dummyHandler);
  }
}
