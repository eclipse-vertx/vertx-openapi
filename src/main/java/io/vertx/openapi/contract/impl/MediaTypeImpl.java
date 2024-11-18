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

import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;

public class MediaTypeImpl implements MediaType {
  private static final String KEY_SCHEMA = "schema";
  private final JsonObject mediaTypeModel;
  private final String identifier;

  private final JsonSchema schema;

  public MediaTypeImpl(String identifier, JsonObject mediaTypeModel) {
    this.identifier = identifier;
    this.mediaTypeModel = mediaTypeModel;
    JsonObject schemaJson = mediaTypeModel.getJsonObject(KEY_SCHEMA);
    if (schemaJson == null || schemaJson.isEmpty()) {
      // Inject default value if schema is left out
      // by using shortcut "application/octet-stream: {}"
      // See https://www.openapis.org/blog/2021/02/16/migrating-from-openapi-3-0-to-3-1-0
      if (identifier.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)) {
        // In OpenAPI v3.0, describing file uploads is signalled with a type:
        // string and the format set to byte, binary, or base64.
        // In OpenAPI v3.1, JSON Schema helps make this far more clear with
        // its contentEncoding and contentMediaType keywords,
        // which are designed for exactly this sort of use.
        // See https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.6
        schemaJson = new JsonObject()
          .put("type", "string")
          .put("format", "binary")
          .put("contentMediaType", "application/octet-stream");
      } else {
        throw createUnsupportedFeature("Media Type without a schema");
      }
    }
    schema = JsonSchema.of(schemaJson);
  }

  @Override
  public JsonSchema getSchema() {
    return schema;
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return mediaTypeModel;
  }
}
