package io.vertx.openapi.impl;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.Operation;

public class OperationImpl implements Operation {
  private final String operationId;
  private final String path;
  private final HttpMethod method;

  private final JsonObject operationModel;

  public OperationImpl(String operationId, HttpMethod method, String path, JsonObject operationModel) {
    this.operationId = operationId;
    this.method = method;
    this.path = path;
    this.operationModel = operationModel;
  }

  @Override public Operation addHandler(Handler<RoutingContext> handler) {
    return null;
  }

  @Override public Operation addFailureHandler(Handler<RoutingContext> handler) {
    return null;
  }

  @Override public String getOperationId() {
    return operationId;
  }

  @Override public JsonObject getOperationModel() {
    return operationModel.copy();
  }

  @Override public HttpMethod getHttpMethod() {
    return method;
  }

  @Override public String getOpenAPIPath() {
    return path;
  }
}
