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

import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.mediatype.MediaTypeRegistration;

public class MediaTypeImpl implements MediaType {
  private static final String KEY_SCHEMA = "schema";
  private final JsonObject mediaTypeModel;
  private final String identifier;
  private final MediaTypeRegistration registration;

  private final JsonSchema schema;

  public MediaTypeImpl(String identifier, JsonObject mediaTypeModel, MediaTypeRegistration registration) {
    this.identifier = identifier;
    this.mediaTypeModel = mediaTypeModel;
    this.registration = registration;

    if (mediaTypeModel == null) {
      throw createUnsupportedFeature("Media Type without a schema");
    }

    boolean emptySchema = mediaTypeModel
        .fieldNames().stream()
        // Ignore all the internal json-schema annotations, they start and end with "__"
        .allMatch(name -> name.startsWith("__") && name.endsWith("__"));

    if (emptySchema) {
      // OpenAPI 3.1 allows defining MediaTypes without a schema.
      schema = null;
    } else {
      JsonObject schemaJson = mediaTypeModel.getJsonObject(KEY_SCHEMA);
      if (schemaJson == null || schemaJson.isEmpty()) {
        throw createUnsupportedFeature("Media Type without a schema");
      }
      schema = JsonSchema.of(schemaJson);

    }
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

  /**
   * The MediaTypeRegistration which is associated to this MediaType.
   * @return the associated MediaTypeRegistration
   */
  public MediaTypeRegistration getRegistration() {
    return registration;
  }
}
