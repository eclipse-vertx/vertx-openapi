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

package io.vertx.openapi.contract;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.JsonSchema;

import java.util.Arrays;
import java.util.List;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#media-type-Object">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#media-type-Object">Operation V3.0</a>
 */
@VertxGen
public interface MediaType extends OpenAPIObject {

  String APPLICATION_HAL_JSON = "application/hal+json";
  String APPLICATION_JSON = HttpHeaderValues.APPLICATION_JSON.toString();
  String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";
  String MULTIPART_FORM_DATA = HttpHeaderValues.MULTIPART_FORM_DATA.toString();
  List<String> SUPPORTED_MEDIA_TYPES = Arrays.asList(APPLICATION_JSON, APPLICATION_JSON_UTF8, MULTIPART_FORM_DATA, APPLICATION_HAL_JSON);

  static boolean isMediaTypeSupported(String type) {
    return SUPPORTED_MEDIA_TYPES.contains(type.toLowerCase());
  }

  /**
   * @return the schema defining the content of the request.
   */
  JsonSchema getSchema();

  /**
   * @return the identifier like <i>application/json</i>
   */
  String getIdentifier();
}
