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
import io.vertx.core.http.HttpServerResponse;
import io.vertx.openapi.validation.ResponseParameter;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

public class ValidatedResponseImpl implements ValidatedResponse {
  private final Map<String, ResponseParameter> headers;
  private final ResponseParameter body;
  private final ValidatableResponse unvalidated;

  public ValidatedResponseImpl(Map<String, ResponseParameter> headers, ResponseParameter body,
                               ValidatableResponse unvalidated) {
    this.headers = safeUnmodifiableMap(headers);
    this.body = body == null ? new RequestParameterImpl(null) : body;
    this.unvalidated = unvalidated;
  }

  protected static Map<String, ResponseParameter> safeUnmodifiableMap(Map<String, ResponseParameter> map) {
    Map<String, ResponseParameter> lowerCaseHeader =
      Optional.ofNullable(map).orElse(emptyMap()).entrySet().stream().collect(toMap(
        entry -> entry.getKey().toLowerCase(), Entry::getValue));

    return Collections.unmodifiableMap(map == null ? Collections.emptyMap() :
      new HashMap<String, ResponseParameter>(lowerCaseHeader) {
        @Override
        public ResponseParameter get(Object key) {
          return super.get(key.toString().toLowerCase());
        }

        @Override
        public boolean containsKey(Object key) {
          return get(key) != null;
        }
      });
  }

  @Override
  public Map<String, ResponseParameter> getHeaders() {
    return headers;
  }

  @Override
  public ResponseParameter getBody() {
    return body;
  }

  @Override
  public Future<Void> send(HttpServerResponse serverResponse) {
    serverResponse.setStatusCode(unvalidated.getStatusCode());

    for (String header : headers.keySet()) {
      ResponseParameter headerValue = unvalidated.getHeaders().get(header);
      if (headerValue != null) {
        serverResponse.headers().add(header, headerValue.getString());
      }
    }

    if (body.isNull() || (body.isString() && body.getString().isEmpty()) || (body.isBuffer() && body.getBuffer().length() == 0)) {
      return serverResponse.send();
    } else {
      serverResponse.headers().add(CONTENT_TYPE.toString(), unvalidated.getContentType());
      return serverResponse.send(unvalidated.getBody().getBuffer());
    }
  }
}
