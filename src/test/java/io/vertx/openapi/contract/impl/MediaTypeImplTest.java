package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContractException;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaTypeImplTest {

  @Test
  void testGetters() {
    JsonObject model = new JsonObject().put("schema", stringSchema().toJson());
    MediaType mediaType = new MediaTypeImpl(model);

    assertThat(mediaType.getOpenAPIModel()).isEqualTo(model);
    assertThat(mediaType.getSchema().fieldNames()).containsExactly("type", "$id");
  }

  @Test
  void testExceptions() {
    String msg = "The passed OpenAPI contract contains a feature that is not supported: Media Type without a schema";

    OpenAPIContractException exceptionNull =
      assertThrows(OpenAPIContractException.class, () -> new MediaTypeImpl(new JsonObject().putNull("schema")));
    assertThat(exceptionNull.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionNull).hasMessageThat().isEqualTo(msg);

    OpenAPIContractException exceptionEmpty =
      assertThrows(OpenAPIContractException.class,
        () -> new MediaTypeImpl(new JsonObject().put("schema", EMPTY_JSON_OBJECT)));
    assertThat(exceptionEmpty.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionEmpty).hasMessageThat().isEqualTo(msg);
  }
}
