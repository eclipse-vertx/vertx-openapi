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

import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
import java.util.Collections;
import java.util.Map;

public class ValidatedRequestImpl implements ValidatedRequest {
  private final Map<String, RequestParameter> cookies;
  private final Map<String, RequestParameter> headers;
  private final Map<String, RequestParameter> path;
  private final Map<String, RequestParameter> query;
  private final RequestParameter body;

  public ValidatedRequestImpl(Map<String, RequestParameter> cookies, Map<String, RequestParameter> headers,
      Map<String, RequestParameter> path, Map<String, RequestParameter> query) {
    this(cookies, headers, path, query, null);
  }

  public ValidatedRequestImpl(Map<String, RequestParameter> cookies, Map<String, RequestParameter> headers,
      Map<String, RequestParameter> path, Map<String, RequestParameter> query, RequestParameter body) {
    this.cookies = safeUnmodifiableMap(cookies);
    this.headers = safeUnmodifiableMap(headers);
    this.path = safeUnmodifiableMap(path);
    this.query = safeUnmodifiableMap(query);
    this.body = body == null ? new RequestParameterImpl(null) : body;
  }

  private static Map<String, RequestParameter> safeUnmodifiableMap(Map<String, RequestParameter> map) {
    return Collections.unmodifiableMap(map == null ? Collections.emptyMap() : map);
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
