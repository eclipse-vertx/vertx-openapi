package io.vertx.openapi.contract;

import io.vertx.core.json.JsonObject;

public interface OpenAPIObject {

  /**
   * Returns the part of the related OpenAPI specification which is represented by the OpenAPI object that is
   * implementing this interface.
   *
   * @return a {@link  JsonObject} that represents this part of the related OpenAPI specification.
   */
  JsonObject getOpenAPIModel();
}
