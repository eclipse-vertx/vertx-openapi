package io.vertx.openapi.validation.impl;

import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestParameters;

import java.util.Collections;
import java.util.Map;

public class RequestParametersImpl implements RequestParameters {
  private final Map<String, RequestParameter> cookies;
  private final Map<String, RequestParameter> headers;
  private final Map<String, RequestParameter> path;
  private final Map<String, RequestParameter> query;
  private final RequestParameter body;

  public RequestParametersImpl(Map<String, RequestParameter> cookies, Map<String, RequestParameter> headers,
    Map<String, RequestParameter> path, Map<String, RequestParameter> query, RequestParameter body) {
    this.cookies = Collections.unmodifiableMap(cookies);
    this.headers = Collections.unmodifiableMap(headers);
    this.path = Collections.unmodifiableMap(path);
    this.query = Collections.unmodifiableMap(query);
    this.body = body;
  }

  @Override
  public Map<String, RequestParameter> getCookies() {
    return cookies;
  }

  @Override
  public Map<String, RequestParameter> getHeaders() {
    return headers;
  }

  @Override
  public Map<String, RequestParameter> getPathParameters() {
    return path;
  }

  @Override
  public Map<String, RequestParameter> getQuery() {
    return query;
  }

  @Override
  public RequestParameter getBody() {
    return body;
  }
}
