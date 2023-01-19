package io.vertx.openapi.validation.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.RequestBody;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.transformer.ApplicationJsonTransformer;
import io.vertx.openapi.validation.transformer.BodyTransformer;
import io.vertx.openapi.validation.transformer.FormTransformer;
import io.vertx.openapi.validation.transformer.LabelTransformer;
import io.vertx.openapi.validation.transformer.MatrixTransformer;
import io.vertx.openapi.validation.transformer.ParameterTransformer;
import io.vertx.openapi.validation.transformer.SimpleTransformer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValue;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValueBody;
import static io.vertx.openapi.validation.ValidatorException.createMissingRequiredParameter;
import static io.vertx.openapi.validation.ValidatorException.createOperationIdInvalid;
import static io.vertx.openapi.validation.ValidatorException.createOperationNotFound;
import static io.vertx.openapi.validation.ValidatorException.createUnsupportedValueFormat;

public class RequestValidatorImpl implements RequestValidator {
  private final Vertx vertx;
  private final OpenAPIContract contract;
  private final Map<Style, ParameterTransformer> parameterTransformers;

  private final Map<String, BodyTransformer> bodyTransformers;

  public RequestValidatorImpl(Vertx vertx, OpenAPIContract contract) {
    this.vertx = vertx;
    this.contract = contract;
    parameterTransformers = new EnumMap<>(Style.class);
    parameterTransformers.put(SIMPLE, new SimpleTransformer());
    parameterTransformers.put(LABEL, new LabelTransformer());
    parameterTransformers.put(MATRIX, new MatrixTransformer());
    parameterTransformers.put(FORM, new FormTransformer());

    bodyTransformers = new HashMap<>();
    bodyTransformers.put(APPLICATION_JSON.toString(), new ApplicationJsonTransformer());
  }

  @Override
  public Future<ValidatedRequest> validate(HttpServerRequest request) {
    Operation operation = contract.findOperation(request.path(), request.method());
    if (operation == null) {
      return failedFuture(createOperationNotFound(request.method(), request.path()));
    }
    return validate(request, operation.getOperationId());
  }

  @Override
  public Future<ValidatedRequest> validate(HttpServerRequest request, String operationId) {
    Operation operation = contract.operation(operationId);
    if (operation == null) {
      return failedFuture(createOperationIdInvalid(operationId));
    }
    return ValidatableRequest.of(request, operation)
      .compose(validatableRequest -> validate(validatableRequest, operationId));
  }

  @Override
  public Future<ValidatedRequest> validate(ValidatableRequest request, String operationId) {
    Operation operation = contract.operation(operationId);
    if (operation == null) {
      return failedFuture(createOperationIdInvalid(operationId));
    }

    return vertx.executeBlocking(p -> {
      Map<String, RequestParameter> cookies = new HashMap<>(request.getCookies().size());
      Map<String, RequestParameter> headers = new HashMap<>(request.getHeaders().size());
      Map<String, RequestParameter> path = new HashMap<>(request.getPathParameters().size());
      Map<String, RequestParameter> query = new HashMap<>(request.getQuery().size());

      for (Parameter param : operation.getParameters()) {
        switch (param.getIn()) {
          case COOKIE:
            cookies.put(param.getName(), validateParameter(param, request.getCookies().get(param.getName())));
            break;
          case HEADER:
            headers.put(param.getName(), validateParameter(param, request.getHeaders().get(param.getName())));
            break;
          case PATH:
            path.put(param.getName(), validateParameter(param, request.getPathParameters().get(param.getName())));
            break;
          case QUERY:
            query.put(param.getName(), validateParameter(param, request.getQuery().get(param.getName())));
        }
      }

      RequestParameter body = validateBody(operation.getRequestBody(), request);
      p.complete(new ValidatedRequestImpl(cookies, headers, path, query, body));
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
    }

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

  // VisibleForTesting
  RequestParameter validateBody(RequestBody requestBody, ValidatableRequest request) {
    if (requestBody == null) {
      return new RequestParameterImpl(null);
    }
    if (request.getBody() == null || request.getBody().isEmpty()) {
      if (requestBody.isRequired()) {
        throw new ValidatorException("The related request does not contain the required body.",
          MISSING_REQUIRED_PARAMETER);
      } else {
        return new RequestParameterImpl(null);
      }
    }

    String mediaTypeIdentifier = request.getContentType();
    MediaType mediaType = requestBody.getContent().get(mediaTypeIdentifier);
    BodyTransformer transformer = bodyTransformers.get(mediaTypeIdentifier);
    if (transformer == null || mediaType == null) {
      throw new ValidatorException("The format of the request body is not supported", UNSUPPORTED_VALUE_FORMAT);
    }
    Object transformedValue = transformer.transform(mediaType, request.getBody().getBuffer());
    OutputUnit result = contract.getSchemaRepository().validator(mediaType.getSchema()).validate(transformedValue);
    if (Boolean.TRUE.equals(result.getValid())) {
      return new RequestParameterImpl(transformedValue);
    }
    throw createInvalidValueBody(result);
  }
}
