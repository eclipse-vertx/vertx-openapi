package io.vertx.openapi.validation.impl;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.openapi.validation.RequestParameters;
import io.vertx.openapi.validation.RequestValidator;

public class RequestValidatorImpl implements RequestValidator {

  @Override
  public Future<RequestParameters> validate(RequestParameters params, String path, HttpMethod method) {
    return null;
  }

  @Override
  public Future<RequestParameters> validate(RequestParameters params, String operationId) {
    return null;
  }
}
