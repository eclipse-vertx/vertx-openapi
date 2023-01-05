package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.impl.RequestValidatorImpl;

@VertxGen
public interface RequestValidator {

  /**
   * Create a new {@link RequestValidator}.
   *
   * @param vertx the related Vert.x instance
   * @return an instance of {@link RequestValidator}.
   */
  static RequestValidator create(Vertx vertx, OpenAPIContract contract) {
    return new RequestValidatorImpl(vertx, contract);
  }

  /**
   * Like {@link #validate(RequestParameters, String)}, but the operationId is determined from the passed path and method.
   *
   * @param params the request parameters to validate.
   * @param path   the path of the request.
   * @param method the method of the request.
   * @return A succeeded Future with the parsed and validated request parameters, or a failed Future containing ValidationException.
   */
  Future<RequestParameters> validate(RequestParameters params, String path, HttpMethod method);

  /**
   * Validates the passed request parameters against the operation defined in the related OpenAPI contract.
   * <p>
   * Passing the related operationId saves the effort to determine it from the combination of path and method.
   *
   * @param params      the request parameters to validate.
   * @param operationId the id of the related operation.
   * @return A succeeded Future with the parsed and validated request parameters, or a failed Future containing ValidationException.
   */
  Future<RequestParameters> validate(RequestParameters params, String operationId);
}
