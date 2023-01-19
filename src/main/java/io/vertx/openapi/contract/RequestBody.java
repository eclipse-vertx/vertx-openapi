package io.vertx.openapi.contract;

import java.util.Map;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#request-body-Object">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#request-body-Object">Operation V3.0</a>
 */
public interface RequestBody extends OpenAPIObject {

  /**
   * @return true if the request body is required in the request, otherwise false.
   */
  boolean isRequired();

  Map<String, MediaType> getContent();
}
