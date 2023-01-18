package io.vertx.openapi.test.e2e;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.router.RouterBuilder;
import io.vertx.openapi.test.base.RouterBuilderTestBase;
import io.vertx.openapi.validation.RequestParameters;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;

class RouterBuilderTest extends RouterBuilderTestBase {

  @ParameterizedTest(name = "{index} should load and mount all operations of an OpenAPI ({0}) contract")
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @ValueSource(strings = {"v3.0", "v3.1"})
  void testRouter(String version, VertxTestContext testContext) {
    Checkpoint cpListPets = testContext.checkpoint(2);
    Checkpoint cpCreatePets = testContext.checkpoint(2);
    Checkpoint cpShowPetById = testContext.checkpoint(2);

    Function<Checkpoint, Handler<RoutingContext>> buildCheckpointHandler = cp -> rc -> {
      RequestParameters parameters = rc.get(RouterBuilder.KEY_META_DATA_VALIDATED_PARAMETERS);
      cp.flag();
      rc.response().send(Json.encode(parameters)).onFailure(testContext::failNow);
    };

    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve(version).resolve("petstore.json");
    createServer(pathDereferencedContract, rb -> {
      rb.operation("listPets").addHandler(buildCheckpointHandler.apply(cpListPets));
      rb.operation("createPets").addHandler(buildCheckpointHandler.apply(cpCreatePets));
      rb.operation("showPetById").addHandler(buildCheckpointHandler.apply(cpShowPetById));
      return Future.succeededFuture(rb);
    }).compose(v -> createRequest(GET, "/pets").addQueryParam("limit", "42").send())
      .onSuccess(response -> testContext.verify(() -> {
        JsonObject query = response.bodyAsJsonObject().getJsonObject("query");
        assertThat(query.getJsonObject("limit").getMap()).containsEntry("long", 42);
        cpListPets.flag();
      }))
      .compose(v -> createRequest(POST, "/pets").send())
      .onSuccess(response -> testContext.verify(() -> {
        JsonObject body = response.bodyAsJsonObject().getJsonObject("body");
        assertThat(body.getMap()).containsEntry("null", true);
        cpCreatePets.flag();
      }))
      .compose(v -> createRequest(GET, "/pets/foobar").send())
      .onSuccess(response -> testContext.verify(() -> {
        JsonObject path = response.bodyAsJsonObject().getJsonObject("pathParameters");
        assertThat(path.getJsonObject("petId").getMap()).containsEntry("string", "foobar");
        cpShowPetById.flag();
      }))
      .onFailure(testContext::failNow);
  }
}
