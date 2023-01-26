package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.JsonSchema;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#media-type-Object">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#media-type-Object">Operation V3.0</a>
 */
@VertxGen
public interface MediaType extends OpenAPIObject {

  /**
   * @return the schema defining the content of the request.
   */
  JsonSchema getSchema();
}
