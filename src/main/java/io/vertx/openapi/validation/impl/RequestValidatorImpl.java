/*
 * Copyright (c) 2023, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.openapi.validation.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static io.vertx.openapi.validation.SchemaValidationException.createErrorFromOutputUnitType;
import static io.vertx.openapi.validation.ValidationContext.REQUEST;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorException.createMissingRequiredParameter;
import static io.vertx.openapi.validation.ValidatorException.createOperationNotFound;
import static io.vertx.openapi.validation.ValidatorException.createUnsupportedValueFormat;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.json.schema.JsonSchemaValidationException;
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
import io.vertx.openapi.validation.transformer.FormTransformer;
import io.vertx.openapi.validation.transformer.LabelTransformer;
import io.vertx.openapi.validation.transformer.MatrixTransformer;
import io.vertx.openapi.validation.transformer.ParameterTransformer;
import io.vertx.openapi.validation.transformer.SimpleTransformer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class RequestValidatorImpl extends BaseValidator implements RequestValidator {
  private final Map<Style, ParameterTransformer> parameterTransformers;

  public RequestValidatorImpl(Vertx vertx, OpenAPIContract contract) {
    super(vertx, contract);
    parameterTransformers = new EnumMap<>(Style.class);
    parameterTransformers.put(SIMPLE, new SimpleTransformer());
    parameterTransformers.put(LABEL, new LabelTransformer());
    parameterTransformers.put(MATRIX, new MatrixTransformer());
    parameterTransformers.put(FORM, new FormTransformer());
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
    return getOperation(operationId).compose(op -> ValidatableRequest.of(request, op))
        .compose(params -> validate(params, operationId));
  }

  @Override
  public Future<ValidatedRequest> validate(ValidatableRequest request, String operationId) {
    return getOperation(operationId).compose(operation -> vertx.executeBlocking(() -> {
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
      return new ValidatedRequestImpl(cookies, headers, path, query, body);
    }));
  }

  // VisibleForTesting
  public RequestParameter validateParameter(Parameter parameter, RequestParameter value) throws ValidatorException {
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
    Object transformedValue = transformer.transform(parameter, String.valueOf(value.get()));

    OutputUnit result = contract
        .getSchemaRepository()
        .validator(parameter.getSchema())
        .validate(transformedValue);

    try {
      result.checkValidity();
      return new RequestParameterImpl(transformedValue);
    } catch (JsonSchemaValidationException e) {
      throw createErrorFromOutputUnitType(parameter, result, e);
    }
  }

  // VisibleForTesting
  public RequestParameter validateBody(RequestBody requestBody, ValidatableRequest request) {
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

    MediaType mediaType = requestBody.determineContentType(request.getContentType());
    Buffer content = request.getBody().getBuffer(Buffer.buffer());

    return validate(mediaType, request.getContentType(), content, REQUEST);
  }
}
