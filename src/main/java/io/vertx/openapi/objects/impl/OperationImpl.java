package io.vertx.openapi.objects.impl;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.objects.Operation;
import io.vertx.openapi.objects.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.objects.impl.ParameterImpl.parseParameters;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class OperationImpl implements Operation {
  private final static Logger LOG = LoggerFactory.getLogger(OperationImpl.class);

  private static final String KEY_OPERATION_ID = "operationId";
  private static final String KEY_TAGS = "tags";
  private static final String KEY_PARAMETERS = "parameters";

  private final String operationId;
  private final String path;
  private final HttpMethod method;
  private final JsonObject operationModel;
  private final List<Parameter> parameters;

  private final List<String> tags;
  private List<Handler<RoutingContext>> handlers = new ArrayList<>();
  private List<Handler<RoutingContext>> failureHandlers = new ArrayList<>();

  public OperationImpl(String path, HttpMethod method, JsonObject operationModel, List<Parameter> pathParameters) {
    this.operationId = operationModel.getString(KEY_OPERATION_ID);
    this.method = method;
    this.path = path;
    this.operationModel = operationModel;

    List<String> tags = operationModel.getJsonArray(KEY_TAGS, EMPTY_JSON_ARRAY).stream().map(Object::toString).collect(
      toList());
    this.tags = unmodifiableList(tags);

    List<Parameter> parameters = parseParameters(path, operationModel.getJsonArray(KEY_PARAMETERS, EMPTY_JSON_ARRAY));
    // pretty sure there is a smarter / more efficient way
    for (Parameter pathParam : pathParameters) {
      Optional<Parameter> parameterDuplicate = parameters.stream()
        .filter(param -> pathParam.getName().equals(param.getName()) && pathParam.getIn().equals(param.getIn()))
        .findAny();

      if (parameterDuplicate.isPresent()) {
        LOG.debug("Found ambiguous parameter (" + pathParam.getName() + ") in operation: " + operationId);
      } else {
        parameters.add(pathParam);
      }
    }

    this.parameters = unmodifiableList(parameters);
  }

  @Override
  public Operation addHandler(Handler<RoutingContext> handler) {
    handlers.add(handler);
    return this;
  }

  @Override
  public Operation addFailureHandler(Handler<RoutingContext> handler) {
    failureHandlers.add(handler);
    return this;
  }

  /**
   * @return handlers of this operation
   */
  public List<Handler<RoutingContext>> getHandlers() {
    return unmodifiableList(handlers);
  }

  /**
   * @return failure handlers of this operation
   */
  public List<Handler<RoutingContext>> getFailureHandlers() {
    return unmodifiableList(failureHandlers);
  }

  @Override
  public String getOperationId() {
    return operationId;
  }

  @Override
  public JsonObject getOperationModel() {
    return operationModel.copy();
  }

  @Override
  public HttpMethod getHttpMethod() {
    return method;
  }

  @Override
  public String getOpenAPIPath() {
    return path;
  }

  @Override
  public List<String> getTags() {
    return tags;
  }

  @Override
  public List<Parameter> getParameters() {
    return parameters;
  }
}
