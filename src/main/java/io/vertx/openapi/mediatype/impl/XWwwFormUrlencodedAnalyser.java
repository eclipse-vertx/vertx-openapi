/*
 * Copyright (c) 2025, Shi HaiBin
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

import static io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistration.APPLICATION_X_WWW_FORM_URL_ENCODED;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.mediatype.MediaTypeInfo;
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.ValidatorException;

public class XWwwFormUrlencodedAnalyser extends AbstractContentAnalyser {

  private JsonObject parsedForm;

  /**
   * Creates a new content analyser.
   *
   * @param contentType the content type.
   * @param content     the content to be analysed.
   * @param context     the context in which the content is used.
   */
  public XWwwFormUrlencodedAnalyser(String contentType, Buffer content, ValidationContext context) {
    super(contentType, content, context);
  }

  @Override
  public void checkSyntacticalCorrectness() {
    if (contentType == null || contentType.isEmpty()
        || !contentType.startsWith(APPLICATION_X_WWW_FORM_URL_ENCODED)) {
      String msg = "The expected application/x-www-form-urlencoded " + requestOrResponse
          + " doesn't contain the required content-type header.";
      throw new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
    }

    Charset charset = resolveCharset(contentType);
    String body = content == null ? "" : content.toString(charset);
    parsedForm = parseFormData(body, charset);
  }

  @Override
  public Object transform() {
    return parsedForm;
  }

  public static Charset resolveCharset(String contentType) {
    if (contentType != null && contentType.contains("charset=")) {
      String charsetName = MediaTypeInfo.of(contentType).parameters().get("charset");
      if (charsetName != null) {
        try {
          return Charset.forName(charsetName);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {
          // fall through to default
        }
      }
    }
    return StandardCharsets.UTF_8;
  }

  private JsonObject parseFormData(String body, Charset charset) {
    JsonObject result = new JsonObject();
    if (body.isEmpty()) {
      return result;
    }

    for (String pair : body.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int idx = pair.indexOf('=');
      String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
      String rawValue = idx >= 0 ? pair.substring(idx + 1) : "";

      String key = URLDecoder.decode(rawKey, charset);
      String decodedValue = URLDecoder.decode(rawValue, charset);
      Object value = coerceValue(decodedValue);
      // Handle array notation: key[]=value1&key[]=value2
      if (key.endsWith("[]")) {
        String arrayKey = key.substring(0, key.length() - 2);
        if (!result.containsKey(arrayKey)) {
          result.put(arrayKey, new JsonArray());
        }
        result.getJsonArray(arrayKey).add(value);
      } else {
        // Handle duplicate keys: if key already exists, convert to array
        if (result.containsKey(key)) {
          Object existing = result.getValue(key);
          if (existing instanceof JsonArray) {
            ((JsonArray) existing).add(value);
          } else {
            result.put(key, new JsonArray().add(existing).add(value));
          }
        } else {
          result.put(key, value);
        }
      }
    }
    return result;
  }

  public static Object coerceValue(String value) {
    try {
      return Json.decodeValue(value);
    } catch (DecodeException e) {
      return value;
    }
  }
}
