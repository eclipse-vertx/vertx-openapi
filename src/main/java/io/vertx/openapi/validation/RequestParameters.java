package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.openapi.contract.Operation;

import java.util.Map;

@VertxGen
public interface RequestParameters extends Parameters {

  /**
   * Creates a new {@link RequestParameters} object based on the passed {@link HttpServerRequest request} and {@link Operation operation}.
   *
   * @param request   The related request
   * @param operation The related operation
   * @return a {@link RequestParameters} object
   */
  static RequestParameters of(HttpServerRequest request, Operation operation) {
    return RequestUtils.extract(request, operation);
  }

  /**
   * @return the path parameters.
   */
  Map<String, RequestParameter> getPathParameters();

  /**
   * @return the query parameters.
   */
  Map<String, RequestParameter> getQuery();
}
