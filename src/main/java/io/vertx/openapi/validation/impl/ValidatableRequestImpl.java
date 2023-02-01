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
import io.vertx.openapi.validation.ValidatableRequest;

import java.util.Map;

public class ValidatableRequestImpl extends ValidatedRequestImpl implements ValidatableRequest {
  private final String contentType;

  public ValidatableRequestImpl(Map<String, RequestParameter> cookies, Map<String, RequestParameter> headers,
    Map<String, RequestParameter> path, Map<String, RequestParameter> query) {
    this(cookies, headers, path, query, null, null);
  }

  public ValidatableRequestImpl(Map<String, RequestParameter> cookies, Map<String, RequestParameter> headers,
    Map<String, RequestParameter> path, Map<String, RequestParameter> query, RequestParameter body,
    String contentType) {
    super(cookies, headers, path, query, body);
    this.contentType = contentType;
  }

  @Override
  public String getContentType() {
    return contentType;
  }
}
