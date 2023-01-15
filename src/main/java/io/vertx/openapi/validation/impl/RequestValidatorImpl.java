package io.vertx.openapi.validation.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestParameters;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.transformer.FormTransformer;
import io.vertx.openapi.validation.transformer.LabelTransformer;
import io.vertx.openapi.validation.transformer.MatrixTransformer;
import io.vertx.openapi.validation.transformer.ParameterTransformer;
import io.vertx.openapi.validation.transformer.SimpleTransformer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValue;
import static io.vertx.openapi.validation.ValidatorException.createMissingRequiredParameter;
import static io.vertx.openapi.validation.ValidatorException.createOperationIdInvalid;
import static io.vertx.openapi.validation.ValidatorException.createOperationNotFound;
import static io.vertx.openapi.validation.ValidatorException.createUnsupportedValueFormat;

public class RequestValidatorImpl implements RequestValidator {
  private final Vertx vertx;
  private final OpenAPIContract contract;
  private final Map<Style, ParameterTransformer> parameterTransformers;

  public RequestValidatorImpl(Vertx vertx, OpenAPIContract contract) {
    this.vertx = vertx;
    this.contract = contract;
    parameterTransformers = new EnumMap<>(Style.class);
    parameterTransformers.put(SIMPLE, new SimpleTransformer());
    parameterTransformers.put(LABEL, new LabelTransformer());
    parameterTransformers.put(MATRIX, new MatrixTransformer());
    parameterTransformers.put(FORM, new FormTransformer());
  }

  @Override
  public Future<RequestParameters> validate(HttpServerRequest request) {
    Operation operation = contract.findOperation(request.path(), request.method());
    if (operation == null) {
      return failedFuture(createOperationNotFound(request.method(), request.path()));
    }
    return validate(request, operation.getOperationId());
  }

  @Override
  public Future<RequestParameters> validate(HttpServerRequest request, String operationId) {
    Operation operation = contract.operation(operationId);
    if (operation == null) {
      return failedFuture(createOperationIdInvalid(operationId));
    }
    return validate(RequestParameters.of(request, operation), operationId);
  }

  @Override
  public Future<RequestParameters> validate(RequestParameters params, String operationId) {
    Operation operation = contract.operation(operationId);
    if (operation == null) {
      return failedFuture(createOperationIdInvalid(operationId));
    }

    return vertx.executeBlocking(p -> {
      Map<String, RequestParameter> cookies = new HashMap<>(params.getCookies().size());
      Map<String, RequestParameter> headers = new HashMap<>(params.getHeaders().size());
      Map<String, RequestParameter> path = new HashMap<>(params.getPathParameters().size());
      Map<String, RequestParameter> query = new HashMap<>(params.getQuery().size());

      for (Parameter param : operation.getParameters()) {
        switch (param.getIn()) {
          case COOKIE:
            cookies.put(param.getName(), validateParameter(param, params.getCookies().get(param.getName())));
            break;
          case HEADER:
            headers.put(param.getName(), validateParameter(param, params.getHeaders().get(param.getName())));
            break;
          case PATH:
            path.put(param.getName(), validateParameter(param, params.getPathParameters().get(param.getName())));
            break;
          case QUERY:
            query.put(param.getName(), validateParameter(param, params.getQuery().get(param.getName())));
        }
      }
      p.complete(new RequestParametersImpl(cookies, headers, path, query, null));
    });
  }

  // VisibleForTesting
  RequestParameter validateParameter(Parameter parameter, RequestParameter value) throws ValidatorException {
    if (value == null || value.isNull()) {
      if (parameter.isRequired()) {
        throw createMissingRequiredParameter(parameter);
      } else {
        return new RequestParameterImpl(null);
      }
    } else {
      ParameterTransformer transformer = parameterTransformers.get(parameter.getStyle());
      if (transformer == null) {
        throw createUnsupportedValueFormat(parameter);
      }
      Object transformedValue = transformer.transform(parameter, value.getString());
      OutputUnit result = contract.getSchemaRepository().validator(parameter.getSchema()).validate(transformedValue);
      if (Boolean.TRUE.equals(result.getValid())) {
        return new RequestParameterImpl(transformedValue);
      }
      throw createInvalidValue(parameter, result);
    }
  }
}
