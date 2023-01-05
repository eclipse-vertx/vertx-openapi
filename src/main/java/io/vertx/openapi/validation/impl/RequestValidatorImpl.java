package io.vertx.openapi.validation.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestParameters;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.validator.PathParameterValidator;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.Location.PATH;

public class RequestValidatorImpl implements RequestValidator {

  private final OpenAPIContract contract;
  private final PathParameterValidator pathParameterValidator;

  private final Vertx vertx;

  public RequestValidatorImpl(Vertx vertx, OpenAPIContract contract) {
    this.vertx = vertx;
    this.contract = contract;
    this.pathParameterValidator = new PathParameterValidator(contract.getSchemaRepository());
  }

  @Override
  public Future<RequestParameters> validate(RequestParameters params, String path, HttpMethod method) {
    return null;
  }

  @Override
  public Future<RequestParameters> validate(RequestParameters params, String operationId) {
    Operation operation = contract.operation(operationId);

    if (operation == null) {
      String msg = "No operation found with id: " + operationId;
      return failedFuture(new IllegalArgumentException(msg));
    }

    return vertx.executeBlocking(p -> {
      Map<String, RequestParameter> cookies = new HashMap<>(params.getCookies().size());
      Map<String, RequestParameter> headers = new HashMap<>(params.getHeaders().size());
      Map<String, RequestParameter> path = new HashMap<>(params.getPathParameters().size());
      Map<String, RequestParameter> query = new HashMap<>(params.getQuery().size());

      for (Parameter param : operation.getParameters()) {
        Location location = param.getIn();

        if (PATH.equals(location)) {
          RequestParameter value = params.getPathParameters().get(param.getName());
          path.put(param.getName(), pathParameterValidator.validate(param, value));
        }
      }

      RequestParameters parametersToReturn = new RequestParametersImpl(cookies, headers, path, query, null);
      p.complete(parametersToReturn);
    });
  }
}
