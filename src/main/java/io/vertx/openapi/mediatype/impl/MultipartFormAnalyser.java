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

package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.ValidatorException;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderValues.MULTIPART_FORM_DATA;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;

public class MultipartFormAnalyser extends AbstractContentAnalyser {
  private static final String BOUNDARY = "boundary=";

  private List<MultipartPart> parts;

  /**
   * Creates a new content analyser.
   *
   * @param contentType the content type.
   * @param content     the content to be analysed.
   * @param context     the context in which the content is used.
   */
  public MultipartFormAnalyser(String contentType, Buffer content, ValidationContext context) {
    super(contentType, content, context);
  }

  // VisibleForTesting
  public static String extractBoundary(String contentType) {
    String[] parts = contentType.split(BOUNDARY, 2);
    if (parts.length == 2 && !parts[1].isBlank()) {
      return parts[1].strip();
    }
    return null;
  }

  @Override
  public void checkSyntacticalCorrectness() {
    if (contentType == null || contentType.isEmpty() || !contentType.startsWith(MULTIPART_FORM_DATA.toString())) {
      String msg = "The expected multipart/form-data " + requestOrResponse + " doesn't contain the required " +
        "content-type header.";
      throw new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
    }

    String boundary = extractBoundary(contentType);
    if (boundary == null) {
      String msg = "The expected multipart/form-data " + requestOrResponse + " doesn't contain the required boundary " +
        "information.";
      throw new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
    }

    parts = MultipartPart.fromMultipartBody(content.toString(), boundary);
  }

  @Override
  public Object transform() {
    JsonObject formData = new JsonObject();
    for (MultipartPart part : parts) {
      if (part.getBody() == null) {
        continue;
      }

      // getContentType() can't be null
      if (part.getContentType().startsWith("text/plain")) {
        try {
          formData.put(part.getName(), Json.decodeValue(part.getBody()));
        } catch (DecodeException de) {
          // Value isn't a number, boolean, etc. -> therefore it is treated as a string.
          Buffer quotedBody = Buffer.buffer("\"").appendBuffer(part.getBody()).appendString("\"");
          formData.put(part.getName(), decodeJsonContent(quotedBody, requestOrResponse));
        }
      } else if (part.getContentType().startsWith("application/json")) {
        formData.put(part.getName(), decodeJsonContent(part.getBody(), requestOrResponse));
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
}
