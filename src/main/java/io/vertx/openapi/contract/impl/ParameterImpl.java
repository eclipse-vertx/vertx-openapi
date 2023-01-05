package io.vertx.openapi.contract.impl;

import io.netty.util.internal.StringUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Style;

import java.util.List;
import java.util.Optional;

import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static java.util.stream.Collectors.toList;

public class ParameterImpl implements Parameter {

  private static final String KEY_NAME = "name";
  private static final String KEY_IN = "in";
  private static final String KEY_REQUIRED = "required";
  private static final String KEY_STYLE = "style";
  private static final String KEY_EXPLODE = "explode";
  private static final String KEY_SCHEMA = "schema";
  private static final String KEY_CONTENT = "content";

  private final String name;
  private final Location in;
  private final boolean required; // Path parameters MUST be true
  private final boolean explode;
  private final JsonObject parameterModel;

  private final JsonSchema schema;
  private Style style;

  public ParameterImpl(String path, JsonObject parameterModel) {
    this.name = parameterModel.getString(KEY_NAME);
    this.required = Optional.ofNullable(parameterModel.getBoolean(KEY_REQUIRED)).orElse(false);
    this.in = Location.parse(parameterModel.getString(KEY_IN));
    this.style =
      Optional.ofNullable(Style.parse(parameterModel.getString(KEY_STYLE))).orElse(Style.defaultByLocation(in));
    if (in == PATH) {
      // if location is "path", name must be part of the path
      if (StringUtil.isNullOrEmpty(name) || !path.contains("{" + name + "}")) {
        throw createInvalidContract("Path parameters MUST have a name that is part of the path");
      }
      // if location is "path", required must be true
      if (!required) {
        throw createInvalidContract("\"required\" MUST be true for path parameters");
      }
      if (!(style == SIMPLE || style == LABEL || style == MATRIX)) {
        throw createInvalidContract("The style of a path parameter MUST be simple, label or matrix");
      }
    }
    this.explode = Optional.ofNullable(parameterModel.getBoolean(KEY_EXPLODE)).orElse(false);
    this.parameterModel = parameterModel;
    JsonObject schemaJson = parameterModel.getJsonObject(KEY_SCHEMA);
    if (schemaJson == null) {
      if (parameterModel.containsKey(KEY_CONTENT)) {
        // Can't find examples for this, therefore I can't test it. Support can be added as soon as it is requested.
        throw createUnsupportedFeature("Usage of property \"content\" in parameter definition");
      }
      throw createInvalidContract("A parameter MUST contain either the \"schema\" or \"content\" property");
    }
    this.schema = JsonSchema.of(schemaJson);
  }

  public static List<Parameter> parseParameters(String path, JsonArray parametersArray) {
    return parametersArray.stream().map(JsonObject.class::cast)
      .map(parameterModel -> new ParameterImpl(path, parameterModel)).collect(
        toList());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Location getIn() {
    return in;
  }

  @Override
  public boolean isRequired() {
    return required;
  }

  @Override
  public Style getStyle() {
    return style;
  }

  @Override
  public boolean isExplode() {
    return explode;
  }

  @Override
  public JsonObject getParameterModel() {
    return parameterModel.copy();
  }

  @Override
  public JsonSchema getSchema() {
    return schema;
  }
}
