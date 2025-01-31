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

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.json.schema.JsonSchema;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#media-type-Object">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#media-type-Object">Operation V3.0</a>
 */
@VertxGen
public interface MediaType extends OpenAPIObject {

  String APPLICATION_JSON = "application/json";
  String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";
  String MULTIPART_FORM_DATA = "multipart/form-data";
  String APPLICATION_HAL_JSON = "application/hal+json";
  String APPLICATION_OCTET_STREAM = "application/octet-stream";
  String TEXT_PLAN = "text/plain";
  String TEXT_PLAIN_UTF8 = TEXT_PLAN + "; charset=utf-8";
  List<String> SUPPORTED_MEDIA_TYPES = Arrays.asList(APPLICATION_JSON, APPLICATION_JSON_UTF8, MULTIPART_FORM_DATA,
    APPLICATION_HAL_JSON, APPLICATION_OCTET_STREAM, TEXT_PLAN, TEXT_PLAIN_UTF8);

  Pattern VENDOR_SPECIFIC_JSON = Pattern.compile("^[^/]+/vnd\\.[\\w.-]+\\+json$");

  static boolean isMediaTypeSupported(String type) {
    return SUPPORTED_MEDIA_TYPES.contains(type.toLowerCase()) || isVendorSpecificJson(type);
  }

  static boolean isVendorSpecificJson(String type) {
    return VENDOR_SPECIFIC_JSON.matcher(type).matches();
  }

  /**
   * This method returns the schema defined in the media type.
   * <p></p>
   * In OpenAPI 3.1 it is allowed to define an empty media type model. In this case the method returns null.
   *
   * @return the schema defined in the media type model, or null in case no media type model was defined.
   */
  JsonSchema getSchema();

  /**
   * @return the identifier like <i>application/json</i>
   */
  String getIdentifier();
}
