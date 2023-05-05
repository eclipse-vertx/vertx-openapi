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

package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Response;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.vertx.openapi.contract.Location.HEADER;
import static io.vertx.openapi.contract.MediaType.SUPPORTED_MEDIA_TYPES;
import static io.vertx.openapi.contract.MediaType.isMediaTypeSupported;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static java.lang.String.join;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class ResponseImpl implements Response {
  private static final String KEY_HEADERS = "headers";
  private static final String KEY_CONTENT = "content";

  private static final Predicate<String> FILTER_CONTENT_TYPE = name -> !name.equalsIgnoreCase(CONTENT_TYPE.toString());

  private final List<Parameter> headers;

  private final Map<String, MediaType> content;

  private final JsonObject responseModel;

  public ResponseImpl(JsonObject responseModel, String operationId) {
    this.responseModel = responseModel;

    JsonObject headersObject = responseModel.getJsonObject(KEY_HEADERS, EMPTY_JSON_OBJECT);
    this.headers = unmodifiableList(
      headersObject
        .fieldNames()
        .stream()
        .filter(JsonSchema.EXCLUDE_ANNOTATIONS)
        .filter(FILTER_CONTENT_TYPE)
        .map(name -> {
          JsonObject headerModel = headersObject.getJsonObject(name).copy().put("name", name).put("in",
            HEADER.toString());
          return new ParameterImpl("", headerModel);
        })
        .collect(Collectors.toList()));

    JsonObject contentObject = responseModel.getJsonObject(KEY_CONTENT, EMPTY_JSON_OBJECT);
    this.content = unmodifiableMap(
      contentObject
        .fieldNames()
        .stream()
        .filter(JsonSchema.EXCLUDE_ANNOTATIONS)
        .collect(toMap(identity(), key -> new MediaTypeImpl(key, contentObject.getJsonObject(key)))));

    if (content.keySet().stream().anyMatch(type -> !isMediaTypeSupported(type))) {
      String msgTemplate = "Operation %s defines a response with an unsupported media type. Supported: %s";
      throw createUnsupportedFeature(String.format(msgTemplate, operationId, join(", ", SUPPORTED_MEDIA_TYPES)));
    }
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return responseModel.copy();
  }

  @Override
  public List<Parameter> getHeaders() {
    return headers;
  }

  @Override
  public Map<String, MediaType> getContent() {
    return content;
  }
}
