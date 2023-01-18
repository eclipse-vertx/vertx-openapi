package io.vertx.openapi.contract;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.ValidationException;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;

@ExtendWith(VertxExtension.class)
class OpenAPIContractTest {

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testFromFailsInvalidSpecMustNotNull(Vertx vertx, VertxTestContext testContext) {
    OpenAPIContract.from(vertx, null).onComplete(testContext.failing(t -> {
      testContext.verify(() -> {
        assertThat(t).isInstanceOf(OpenAPIContractException.class);
        assertThat(t).hasMessageThat().isEqualTo("The passed OpenAPI contract is invalid: Spec must not be null");
        testContext.completeNow();
      });
    }));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testFromFailsInvalidSpec(Vertx vertx, VertxTestContext testContext) {
    Path path = getRelatedTestResourcePath(OpenAPIContractTest.class).resolve("v3_0_invalid_petstore.json");
    JsonObject invalidContractJson = vertx.fileSystem().readFileBlocking(path.toString()).toJsonObject();

    OpenAPIContract.from(vertx, invalidContractJson).onComplete(testContext.failing(t -> {
      testContext.verify(() -> {
        assertThat(t).isInstanceOf(OpenAPIContractException.class);
        assertThat(t).hasMessageThat().isEqualTo("The passed OpenAPI contract is invalid.");
        assertThat(t).hasCauseThat().isInstanceOf(ValidationException.class);
        testContext.completeNow();
      });
    }));
  }
}
