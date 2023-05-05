package io.vertx.openapi.validation.transformer;

import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

import io.vertx.json.schema.JsonSchema;

interface SchemaSupport {

  String NAME = "dummy";

  JsonSchema OBJECT_SCHEMA = JsonSchema.of(objectSchema().toJson());
  JsonSchema ARRAY_SCHEMA = JsonSchema.of(arraySchema().toJson());
  JsonSchema STRING_SCHEMA = JsonSchema.of(stringSchema().toJson());
  JsonSchema NUMBER_SCHEMA = JsonSchema.of(numberSchema().toJson());
  JsonSchema INTEGER_SCHEMA = JsonSchema.of(intSchema().toJson());
  JsonSchema BOOLEAN_SCHEMA = JsonSchema.of(booleanSchema().toJson());
}
