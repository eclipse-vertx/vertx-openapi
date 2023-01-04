package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#operationObject">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#operationObject">Operation V3.0</a>
 */
@VertxGen
public interface Operation {

  /**
   * Adds a handler for this operation which is executed after the security and validation handlers defined in the contract
   *
   * @param handler The handler to add
   * @return the operation
   */
  @Fluent
  Operation addHandler(Handler<RoutingContext> handler);

  /**
   * @return handlers of this operation
   */
  @GenIgnore
  List<Handler<RoutingContext>> getHandlers();

  /**
   * Adds a failure handler for this operation
   *
   * @param handler The failure handler to add
   * @return the operation
   */
  @Fluent
  Operation addFailureHandler(Handler<RoutingContext> handler);

  /**
   * @return failure handlers of this operation
   */
  @GenIgnore
  List<Handler<RoutingContext>> getFailureHandlers();

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

  /**
   * @return tags of this operation
   */
  List<String> getTags();

  /**
   * @return parameters of this operation
   */
  List<Parameter> getParameters();
}
