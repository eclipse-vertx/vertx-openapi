package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.openapi.contract.Operation;

@VertxGen
public interface ValidatableRequest extends ValidatedRequest {

  /**
   * Creates a new {@link ValidatableRequest} object based on the passed {@link HttpServerRequest request} and {@link Operation operation}.
   *
   * @param request   The related request
   * @param operation The related operation
   * @return a {@link ValidatableRequest} object
   */
  static Future<ValidatableRequest> of(HttpServerRequest request, Operation operation) {
    return RequestUtils.extract(request, operation);
  }

  String getContentType();
}
