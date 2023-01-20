package io.vertx.openapi.router;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.validation.ValidatableRequest;

public interface RequestExtractor {

  /**
   * Extracts and transforms the parameters and the body of an incoming request into a {@link ValidatableRequest format}
   * that can be validated by the {@link io.vertx.openapi.validation.RequestValidator}.
   *
   * @param routingContext The routing context of the incoming request.
   * @param operation      The operation of the related request.
   * @return A {@link Future} holding the {@link ValidatableRequest}.
   */
  Future<ValidatableRequest> extractValidatableRequest(RoutingContext routingContext, Operation operation);
}
