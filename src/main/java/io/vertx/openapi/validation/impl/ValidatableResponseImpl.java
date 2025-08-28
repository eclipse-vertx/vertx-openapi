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

import io.vertx.openapi.validation.ResponseParameter;
import io.vertx.openapi.validation.ValidatableResponse;
import java.util.Map;

public class ValidatableResponseImpl extends ValidatedResponseImpl implements ValidatableResponse {

  private final String contentType;

  private final int statusCode;

  public ValidatableResponseImpl(int statusCode, Map<String, ResponseParameter> headers) {
    this(statusCode, headers, null, null);
  }

  public ValidatableResponseImpl(int statusCode, Map<String, ResponseParameter> headers, ResponseParameter body,
      String contentType) {
    super(headers, body, null);
    this.statusCode = statusCode;
    this.contentType = contentType;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }
}
