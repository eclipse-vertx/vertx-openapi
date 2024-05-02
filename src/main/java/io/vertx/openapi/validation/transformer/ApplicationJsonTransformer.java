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

package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;

public class ApplicationJsonTransformer implements BodyTransformer {

  @Override
  public Object transformRequest(MediaType type, ValidatableRequest request) {
    return transform(type, request.getBody().getBuffer());
  }

  @Override
  public Object transformResponse(MediaType type, ValidatableResponse response) {
    return transform(type, response.getBody().getBuffer());
  }

  // used in MultipartFormTransformer
  Object transform(MediaType type, Buffer body) {
    try {
      return Json.decodeValue(body);
    } catch (DecodeException e) {
      throw new ValidatorException("The request body can't be decoded", ILLEGAL_VALUE);
    }
  }
}
