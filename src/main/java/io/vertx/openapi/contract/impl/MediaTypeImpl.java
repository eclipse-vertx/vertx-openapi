package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.MediaType;

import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;

public class MediaTypeImpl implements MediaType {
  private static final String KEY_SCHEMA = "schema";

  private final JsonObject mediaTypeModel;

  private final JsonSchema schema;

  public MediaTypeImpl(JsonObject mediaTypeModel) {
    this.mediaTypeModel = mediaTypeModel;
    JsonObject schemaJson = mediaTypeModel.getJsonObject(KEY_SCHEMA);
    if (schemaJson == null || schemaJson.isEmpty()) {
      throw createUnsupportedFeature("Media Type without a schema");
    }
    schema = JsonSchema.of(schemaJson);
  }

  @Override
  public JsonSchema getSchema() {
    return schema;
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return mediaTypeModel.copy();
  }
}
