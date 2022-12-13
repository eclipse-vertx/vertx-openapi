package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;

import java.util.Map;

@VertxGen
public interface RequestParameters extends Parameters {

  /**
   * @return the path parameters.
   */
  Map<String, RequestParameter> getPathParameters();

  /**
   * @return the query parameters.
   */
  Map<String, RequestParameter> getQuery();
}
