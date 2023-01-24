package io.vertx.openapi;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;

@ExtendWith(VertxExtension.class)
class UtilsTest {

  private static Stream<Arguments> testReadYamlOrJson() throws IOException {
    Path petstoreYaml = getRelatedTestResourcePath(UtilsTest.class).resolve("petstore.yaml");
    Path petstoreJson = getRelatedTestResourcePath(UtilsTest.class).resolve("petstore.json");
    JsonObject expectedJson = Buffer.buffer(Files.readAllBytes(petstoreJson)).toJsonObject();
    return Stream.of(
      Arguments.of(petstoreYaml.toString(), expectedJson),
      Arguments.of(petstoreJson.toString(), expectedJson)
    );
  }

  @ParameterizedTest(name = "{index} test testReadYamlOrJson with: {0}")
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @MethodSource
  void testReadYamlOrJson(String path, JsonObject expected, Vertx vertx, VertxTestContext testContext) {
    Utils.readYamlOrJson(vertx, path).onComplete(testContext.succeeding(json -> testContext.verify(() -> {
      assertThat(json).isEqualTo(expected);
      testContext.completeNow();
    })));
  }
}
