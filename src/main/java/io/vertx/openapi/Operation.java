package io.vertx.openapi;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Interface representing an <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#operationObject">Operation</a>
 */
@VertxGen
public interface Operation {

  /**
   * Adds a handler for this operation which is executed after the security and validation handlers defined in the contract
   *
   * @param handler
   * @return
   */
  @Fluent Operation addHandler(Handler<RoutingContext> handler);

  /**
   * Adds a failure handler for this operation
   *
   * @param handler
   * @return
   */
  @Fluent Operation addFailureHandler(Handler<RoutingContext> handler);


  /**
   * @return operationId of this operation
   */
  String getOperationId();

  /**
   * @return model of this operation
   */
  JsonObject getOperationModel();

  /**
   * @return http method of this operation
   */
  HttpMethod getHttpMethod();

  /**
   * @return path in OpenAPI style
   */
  String getOpenAPIPath();
}
