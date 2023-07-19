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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Response;
import io.vertx.openapi.validation.ResponseParameter;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedResponse;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.transformer.BodyTransformer;
import io.vertx.openapi.validation.transformer.ParameterTransformer;
import io.vertx.openapi.validation.transformer.SimpleTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValue;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValueBody;
import static io.vertx.openapi.validation.ValidatorException.createMissingRequiredParameter;
import static io.vertx.openapi.validation.ValidatorException.createResponseNotFound;

public class ResponseValidatorImpl extends BaseValidator implements ResponseValidator {
  private static final ParameterTransformer TRANSFORMER = new SimpleTransformer();

  public ResponseValidatorImpl(Vertx vertx, OpenAPIContract contract) {
    super(vertx, contract);
  }

  // VisibleForTesting
  Future<Response> getResponse(ValidatableResponse params, String operationId) {
    return getOperation(operationId).compose(op -> {
      Response response = Optional.ofNullable(op.getResponse(params.getStatusCode())).orElse(op.getDefaultResponse());
      if (response == null) {
        return failedFuture(createResponseNotFound(params.getStatusCode(), operationId));
      }
      return succeededFuture(response);
    });
  }

  @Override
  public Future<ValidatedResponse> validate(ValidatableResponse params, String operationId) {
    return getResponse(params, operationId).compose(response -> vertx.executeBlocking(() -> {
      Map<String, ResponseParameter> headers = new HashMap<>(params.getHeaders().size());
      for (Parameter header : response.getHeaders()) {
        headers.put(header.getName(), validateParameter(header, params.getHeaders().get(header.getName())));
      }

      ResponseParameter body = validateBody(response, params);
      return new ValidatedResponseImpl(headers, body, params);
    }));
  }

  // VisibleForTesting
  ResponseParameter validateParameter(Parameter parameter, ResponseParameter value) throws ValidatorException {
    if (value == null || value.isNull()) {
      if (parameter.isRequired()) {
        throw createMissingRequiredParameter(parameter);
      } else {
        return new RequestParameterImpl(null);
      }
    }
    Object transformedValue = TRANSFORMER.transform(parameter, String.valueOf(value.get()));

    OutputUnit result = contract.getSchemaRepository().validator(parameter.getSchema()).validate(transformedValue);

    try {
      result.checkValidity();
      return new RequestParameterImpl(transformedValue);
    } catch (JsonSchemaValidationException e) {
      throw createInvalidValue(parameter, e);
    }
  }

  // VisibleForTesting
  ResponseParameter validateBody(Response response, ValidatableResponse params) {
    if (response.getContent().isEmpty()) {
      return new RequestParameterImpl(null);
    }
    if (params.getBody() == null || params.getBody().isEmpty()) {
      throw new ValidatorException("The related response does not contain the required body.",
        MISSING_REQUIRED_PARAMETER);
    }

    String mediaTypeIdentifier = params.getContentType();
    MediaType mediaType = response.getContent().get(mediaTypeIdentifier);
    BodyTransformer transformer = bodyTransformers.get(mediaTypeIdentifier);
    if (transformer == null || mediaType == null) {
      throw new ValidatorException("The format of the response body is not supported", UNSUPPORTED_VALUE_FORMAT);
    }

    Object transformedValue = transformer.transform(mediaType, params.getBody().getBuffer());
    OutputUnit result = contract.getSchemaRepository().validator(mediaType.getSchema()).validate(transformedValue);

    try {
      result.checkValidity();
      return new RequestParameterImpl(transformedValue);
    } catch (JsonSchemaValidationException e) {
      throw createInvalidValueBody(e);
    }
  }
}
