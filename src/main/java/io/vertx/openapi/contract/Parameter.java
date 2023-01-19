package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaType;

/**
 * This interface represents the most important attributes of an OpenAPI Parameter.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#parameter-Object">Parameter V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#parameter-Object">Parameter V3.0</a>
 */
@VertxGen
public interface Parameter extends OpenAPIObject {

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
   * @return the {@link JsonSchema} of the parameter
   */
  JsonSchema getSchema();

  /**
   * @return the {@link SchemaType} of the parameter
   */
  SchemaType getSchemaType();
}
