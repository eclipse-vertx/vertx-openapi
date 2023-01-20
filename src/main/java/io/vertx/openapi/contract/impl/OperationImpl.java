package io.vertx.openapi.contract.impl;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.RequestBody;

import java.util.List;
import java.util.Optional;

import static io.vertx.json.schema.common.dsl.SchemaType.OBJECT;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.impl.ParameterImpl.parseParameters;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class OperationImpl implements Operation {
  private static final Logger LOG = LoggerFactory.getLogger(OperationImpl.class);

  private static final String KEY_OPERATION_ID = "operationId";
  private static final String KEY_TAGS = "tags";
  private static final String KEY_PARAMETERS = "parameters";

  private static final String KEY_REQUEST_BODY = "requestBody";

  private final String operationId;
  private final String path;
  private final HttpMethod method;
  private final JsonObject operationModel;
  private final List<Parameter> parameters;
  private final RequestBody requestBody;
  private final List<String> tags;

  public OperationImpl(String path, HttpMethod method, JsonObject operationModel, List<Parameter> pathParameters) {
    this.operationId = operationModel.getString(KEY_OPERATION_ID);
    this.method = method;
    this.path = path;
    this.operationModel = operationModel;

    this.tags =
      unmodifiableList(operationModel.getJsonArray(KEY_TAGS, EMPTY_JSON_ARRAY).stream().map(Object::toString).collect(
        toList()));

    List<Parameter> operationParameters =
      parseParameters(path, operationModel.getJsonArray(KEY_PARAMETERS, EMPTY_JSON_ARRAY));
    // pretty sure there is a smarter / more efficient way
    for (Parameter pathParam : pathParameters) {
      Optional<Parameter> parameterDuplicate = operationParameters.stream()
        .filter(param -> pathParam.getName().equals(param.getName()) && pathParam.getIn().equals(param.getIn()))
        .findAny();

      if (parameterDuplicate.isPresent()) {
        LOG.debug("Found ambiguous parameter (" + pathParam.getName() + ") in operation: " + operationId);
      } else {
        operationParameters.add(pathParam);
      }
    }

    long explodedQueryParams =
      operationParameters.stream()
        .filter(p -> p.isExplode() && p.getStyle() == FORM && p.getIn() == QUERY && p.getSchemaType() == OBJECT)
        .count();
    if (explodedQueryParams > 1) {
      String msg =
        "Found multiple exploded query parameters of style form with type object in operation: " + operationId;
      throw OpenAPIContractException.createInvalidContract(msg);
    }

    this.parameters = unmodifiableList(operationParameters);

    JsonObject requestBodyJson = operationModel.getJsonObject(KEY_REQUEST_BODY);
    if (requestBodyJson == null || requestBodyJson.isEmpty()) {
      this.requestBody = null;
    } else {
      this.requestBody = new RequestBodyImpl(requestBodyJson, operationId);
    }
  }

  @Override
  public String getOperationId() {
    return operationId;
  }

  @Override
  public JsonObject getOpenAPIModel() {
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

  @Override
  public RequestBody getRequestBody() {
    return requestBody;
  }
}
