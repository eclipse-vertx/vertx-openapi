package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;

/**
 * This interface represents the most important attributes of an OpenAPI Parameter.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#parameterObject">Parameter V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#parameterObject">Parameter V3.0</a>
 */
@VertxGen
public interface Parameter {

  /**
   * @return name of this parameter
   */
  String getName();

  /**
   * @return location of this parameter
   */
  Location getIn();

  /**
   * @return true if the parameter is required, otherwise false;
   */
  boolean isRequired();

  /**
   * @return style of this parameter
   */
  Style getStyle();

  /**
   * @return true if the parameter should become exploded, otherwise false;
   */
  boolean isExplode();

  /**
   * @return model of this parameter
   */
  JsonObject getParameterModel();

  /**
   * @return the {@link JsonSchema} of the parameter
   */
  JsonSchema getSchema();
}
