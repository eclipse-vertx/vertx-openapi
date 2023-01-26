package io.vertx.openapi.router;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.contract.Operation;

import java.util.List;

@VertxGen
public interface OpenAPIRoute {

  /**
   * Adds a handler for this route which is executed after the security and validation handlers defined in the contract
   *
   * @param handler The handler to add
   * @return the route
   */
  @Fluent
  OpenAPIRoute addHandler(Handler<RoutingContext> handler);

  /**
   * @return handlers of this route
   */
  @GenIgnore
  List<Handler<RoutingContext>> getHandlers();

  /**
   * Adds a failure handler for this route
   *
   * @param handler The failure handler to add
   * @return the route
   */
  @Fluent
  OpenAPIRoute addFailureHandler(Handler<RoutingContext> handler);

  /**
   * @return failure handlers of this route
   */
  @GenIgnore
  List<Handler<RoutingContext>> getFailureHandlers();

  /**
   * @return the related operation of this route
   */
  Operation getOperation();

  /**
   * @return true if validation based on the OpenAPI contract is active for this route. By default, it is active.
   */
  boolean doValidation();

  /**
   * Sets the validation flag for incoming requests.
   *
   * @param doValidation The validation flag.
   * @return the route
   */
  OpenAPIRoute setDoValidation(boolean doValidation);
}
