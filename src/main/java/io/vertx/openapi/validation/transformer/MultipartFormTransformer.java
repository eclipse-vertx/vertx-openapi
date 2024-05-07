/*
 * Copyright (c) 2024, SAP SE
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
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatorException;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderValues.MULTIPART_FORM_DATA;
import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;

public class MultipartFormTransformer implements BodyTransformer {
  private static final String BOUNDARY = "boundary=";
  private static final ApplicationJsonTransformer JSON_TRANSFORMER = new ApplicationJsonTransformer();

  static String extractBoundary(String contentType) {
    String[] parts = contentType.split(BOUNDARY, 2);
    if (parts.length == 2 && !parts[1].isBlank()) {
      return parts[1].strip();
    }
    return null;
  }

  static Object transform(MediaType type, Buffer body, String contentType, String responseOrRequest) {
    if (contentType == null || contentType.isEmpty() || !contentType.startsWith(MULTIPART_FORM_DATA.toString())) {
      String msg = "The expected multipart/form-data " + responseOrRequest + " doesn't contain the required " +
        "content-type header.";
      throw new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
    }

    String boundary = extractBoundary(contentType);
    if (boundary == null) {
      String msg = "The expected multipart/form-data " + responseOrRequest + " doesn't contain the required boundary " +
        "information.";
      throw new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
    }

    JsonObject formData = new JsonObject();

    List<MultipartPart> parts = MultipartPart.fromMultipartBody(body.toString(), boundary);
    for (MultipartPart part : parts) {
      if (part.getBody() == null) {
        continue;
      }

      // getContentType() can't be null
      if (part.getContentType().startsWith("text/plain")) {
        try {
          formData.put(part.getName(), JSON_TRANSFORMER.transform(null, part.getBody()));
        } catch (ValidatorException ve) {
          if (ve.type() == ILLEGAL_VALUE) {
            // Value isn't a number, boolean, etc. -> therefore it is treated as a string.
            Buffer quotedBody = Buffer.buffer("\"").appendBuffer(part.getBody()).appendString("\"");
            formData.put(part.getName(), JSON_TRANSFORMER.transform(null, quotedBody));
          } else {
            throw ve;
          }
        }
      } else if (part.getContentType().startsWith("application/json")) {
        formData.put(part.getName(), JSON_TRANSFORMER.transform(null, part.getBody()));
      } else if (part.getContentType().startsWith("application/octet-stream")) {
        formData.put(part.getName(), part.getBody());
      } else {
        String msg = String.format("The content type %s of property %s is not yet supported.",
          part.getContentType(), part.getName());
        throw new ValidatorException(msg, UNSUPPORTED_VALUE_FORMAT);
      }
    }

    return formData;
  }

  @Override
  public Object transformRequest(MediaType type, ValidatableRequest request) {
    return transform(type, request.getBody().getBuffer(), request.getContentType(), "request");
  }

  @Override
  public Object transformResponse(MediaType type, ValidatableResponse response) {
    return transform(type, response.getBody().getBuffer(), response.getContentType(), "response");
  }
}
