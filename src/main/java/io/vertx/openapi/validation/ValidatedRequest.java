package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;

import java.util.Map;

@VertxGen
public interface ValidatedRequest {

  /**
   * @return the path parameters.
   */
  Map<String, RequestParameter> getPathParameters();

  /**
   * @return the query parameters.
   */
  Map<String, RequestParameter> getQuery();

  /**
   * @return the cookie parameters.
   */
  Map<String, RequestParameter> getCookies();

  /**
   * @return the header parameters.
   */
  Map<String, RequestParameter> getHeaders();

  /**
   * @return the body.
   */
  RequestParameter getBody();
}
