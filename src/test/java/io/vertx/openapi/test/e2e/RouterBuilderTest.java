package io.vertx.openapi.test.e2e;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.router.RouterBuilder;
import io.vertx.openapi.test.base.RouterBuilderTestBase;
import io.vertx.openapi.validation.ValidatedRequest;
import org.junit.jupiter.api.Test;
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
      ValidatedRequest validatedRequest = rc.get(RouterBuilder.KEY_META_DATA_VALIDATED_REQUEST);
      cp.flag();
      rc.response().send(Json.encode(validatedRequest)).onFailure(testContext::failNow);
    };

    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve(version).resolve("petstore.json");
    createServer(pathDereferencedContract, rb -> {
      rb.getRoute("listPets").addHandler(buildCheckpointHandler.apply(cpListPets));
      rb.getRoute("createPets").addHandler(buildCheckpointHandler.apply(cpCreatePets));
      rb.getRoute("showPetById").addHandler(buildCheckpointHandler.apply(cpShowPetById));
      return Future.succeededFuture(rb);
    }).compose(v -> createRequest(GET, "/pets").addQueryParam("limit", "42").send())
      .onSuccess(response -> testContext.verify(() -> {
        JsonObject query = response.bodyAsJsonObject().getJsonObject("query");
        assertThat(query.getJsonObject("limit").getMap()).containsEntry("long", 42);
        cpListPets.flag();
      }))
      .compose(v -> {
        JsonObject bodyJson = new JsonObject().put("id", 1).put("name", "FooBar");
        return createRequest(POST, "/pets").sendJsonObject(bodyJson).onSuccess(response -> testContext.verify(() -> {
          JsonObject body = response.bodyAsJsonObject().getJsonObject("body");
          JsonObject bodyValueAsJson = body.getJsonObject("jsonObject");
          assertThat(bodyValueAsJson).isEqualTo(bodyJson);
          cpCreatePets.flag();
        }));
      })
      .compose(v -> createRequest(GET, "/pets/foobar").send())
      .onSuccess(response -> testContext.verify(() -> {
        JsonObject path = response.bodyAsJsonObject().getJsonObject("pathParameters");
        assertThat(path.getJsonObject("petId").getMap()).containsEntry("string", "foobar");
        cpShowPetById.flag();
      }))
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testRouterWithoutValidation(VertxTestContext testContext) {
    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    createServer(pathDereferencedContract, rb -> {
      rb.rootHandler(BodyHandler.create()).getRoute("createPets").setDoValidation(false)
        .addHandler(rc -> rc.response().end(rc.body().buffer()));
      return Future.succeededFuture(rb);
    }).compose(v -> {
        JsonObject invalidBodyJson = new JsonObject().put("foo", "bar");
        return createRequest(POST, "/pets").sendJsonObject(invalidBodyJson)
          .onSuccess(response -> testContext.verify(() -> {
            assertThat(response.bodyAsJsonObject()).isEqualTo(invalidBodyJson);
            testContext.completeNow();
          }));
      })
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testRouterWithInvalidRequest(VertxTestContext testContext) {
    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    createServer(pathDereferencedContract, rb -> {
      rb.getRoute("createPets").addHandler(rc -> rc.response().end(rc.body().buffer()));
      return Future.succeededFuture(rb);
    }).compose(v -> {
        JsonObject invalidBodyJson = new JsonObject().put("foo", "bar");
        return createRequest(POST, "/pets").sendJsonObject(invalidBodyJson)
          .onSuccess(response -> testContext.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            testContext.completeNow();
          }));
      })
      .onFailure(testContext::failNow);
  }
}
