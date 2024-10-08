/*
 * Copyright (c) 2024, Lucimber UG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.openapi.contract.MediaType.APPLICATION_OCTET_STREAM;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;

public class ApplicationOctetStreamTransformer implements BodyTransformer {

  @Override
  public Object transformRequest(MediaType type, ValidatableRequest request) {
    return transform(type, request.getBody().getBuffer(), request.getContentType(), "request");
  }

  @Override
  public Object transformResponse(MediaType type, ValidatableResponse response) {
    return transform(type, response.getBody().getBuffer(), response.getContentType(), "response");
  }

  private Object transform(MediaType type, Buffer body, String contentType,
                           String responseOrRequest) {
    if (contentType == null || contentType.isEmpty()
      || !contentType.equalsIgnoreCase(APPLICATION_OCTET_STREAM)) {
      String msg = "The " + responseOrRequest
        + " doesn't contain the required content-type header application/octet-stream.";
      throw new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
    }
    return body;
  }
}
