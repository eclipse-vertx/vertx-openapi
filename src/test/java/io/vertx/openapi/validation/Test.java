package io.vertx.openapi.validation;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.ValidationException;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.google.common.truth.Truth.assertThat;

@ExtendWith(VertxExtension.class)
class TestClass {

  @Test
  void test(Vertx vertx, VertxTestContext testContext) {
    OpenAPIVersion.V3_1.getRepository(vertx, "app://").onComplete(testContext.succeeding(repo -> {
      JsonSchema schema = JsonSchema.of(new JsonObject().put("type", "number"));
      Object lol = Json.decodeValue("12.05");
      OutputUnit ou = repo.validator(schema).validate(12.05);
      ValidationException ex = ou.toException(12);
      testContext.verify(() -> assertThat(ou.getValid()).isTrue());
      testContext.completeNow();
    }));
  }
}
