package io.vertx.openapi.objects;

import io.vertx.codegen.annotations.VertxGen;

import java.util.List;

/**
 * This interface represents the most important attributes of an OpenAPI Parameter.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#path-item-object">Parameter V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#path-item-object">Parameter V3.0</a>
 */
@VertxGen
public interface Path {

  /**
   * @return the name of this path
   */
  String getName();

  /**
   * @return operations of this path
   */
  List<Operation> getOperations();

  /**
   * @return parameters of this path
   */
  List<Parameter> getParameters();
}
