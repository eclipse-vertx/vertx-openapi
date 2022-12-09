package io.vertx.openapi.test.e2e;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.test.base.RouterBuilderTestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;

class RouterBuilderTest extends RouterBuilderTestBase {

  @ParameterizedTest(name = "{index} should load and mount all operations of an OpenAPI ({0}) contract")
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @ValueSource(strings = {"v3.0", "v3.1"})
  void testRouter(String version, VertxTestContext testContext) {
    Checkpoint cpListPets = testContext.checkpoint();
    Checkpoint cpCreatePets = testContext.checkpoint();
    Checkpoint cpShowPetById = testContext.checkpoint();

    Function<Checkpoint, Handler<RoutingContext>> buildCheckpointHandler = cp -> rc -> {
      cp.flag();
      rc.end();
    };

    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve(version).resolve("petstore.json");
    createServer(pathDereferencedContract, rb -> {
      rb.operation("listPets").addHandler(buildCheckpointHandler.apply(cpListPets));
      rb.operation("createPets").addHandler(buildCheckpointHandler.apply(cpCreatePets));
      rb.operation("showPetById").addHandler(buildCheckpointHandler.apply(cpShowPetById));
      return Future.succeededFuture(rb);
    }).compose(v -> createRequest(GET, "/pets").send())
      .compose(v -> createRequest(POST, "/pets").send())
      .compose(v -> createRequest(GET, "/pets/1337").send())
      .onFailure(testContext::failNow);
  }
}
