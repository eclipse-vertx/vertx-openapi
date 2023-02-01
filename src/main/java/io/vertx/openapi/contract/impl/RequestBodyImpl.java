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
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;
import static java.lang.String.join;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;

public class RequestBodyImpl implements RequestBody {
  private static final String KEY_REQUIRED = "required";
  private static final String KEY_CONTENT = "content";

  private static final List<String> SUPPORTED_MEDIA_TYPES = singletonList(APPLICATION_JSON.toString());

  private final JsonObject requestBodyModel;

  private final boolean required;

  private final Map<String, MediaType> content;

  public RequestBodyImpl(JsonObject requestBodyModel, String operationId) {
    this.requestBodyModel = requestBodyModel;
    this.required = requestBodyModel.getBoolean(KEY_REQUIRED, false);
    JsonObject contentObject = requestBodyModel.getJsonObject(KEY_CONTENT, EMPTY_JSON_OBJECT);
    this.content = unmodifiableMap(contentObject.getMap().keySet().stream()
      .collect(Collectors.toMap(key -> key, key -> new MediaTypeImpl(contentObject.getJsonObject(key)))));
    if (content.isEmpty()) {
      String msg =
        String.format("Operation %s defines a request body without or with empty property \"content\"", operationId);
      throw createInvalidContract(msg);
    } else if (content.keySet().stream().anyMatch(type -> !isMediaTypeSupported(type))) {
      String msgTemplate = "Operation %s defines a request body with an unsupported media type. Supported: %s";
      throw createUnsupportedFeature(String.format(msgTemplate, operationId, join(",", SUPPORTED_MEDIA_TYPES)));
    }
  }

  private static boolean isMediaTypeSupported(String type) {
    return SUPPORTED_MEDIA_TYPES.contains(type.toLowerCase());
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return requestBodyModel.copy();
  }

  @Override
  public boolean isRequired() {
    return required;
  }

  @Override
  public Map<String, MediaType> getContent() {
    return content;
  }
}
