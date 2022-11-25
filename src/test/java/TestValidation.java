import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.RouterBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class TestValidation {

  @Test
  void test(Vertx vertx, VertxTestContext testContext) {

    JsonObject j = vertx.fileSystem().readFileBlocking("v3.1/petstore.json").toJsonObject();

    RouterBuilder.create(vertx, j).onComplete(testContext.succeedingThenComplete());
  }
}
