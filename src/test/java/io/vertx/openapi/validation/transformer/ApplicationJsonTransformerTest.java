package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationJsonTransformerTest {
  private final BodyTransformer transformer = new ApplicationJsonTransformer();

  @Test
  void testTransform() {
    JsonObject dummyBody = new JsonObject().put("foo", "bar");
    assertThat(transformer.transform(null, dummyBody.toBuffer())).isEqualTo(dummyBody);
  }

  @Test
  void testTransformThrows() {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> transformer.transform(null, Buffer.buffer("\"foobar")));
    String expectedMsg = "The request body can't be decoded";
    assertThat(exception.type()).isEqualTo(ILLEGAL_VALUE);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
