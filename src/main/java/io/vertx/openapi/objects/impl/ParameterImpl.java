package io.vertx.openapi.objects.impl;

import io.netty.util.internal.StringUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.RouterBuilderException;
import io.vertx.openapi.objects.Location;
import io.vertx.openapi.objects.Parameter;
import io.vertx.openapi.objects.Style;

import java.util.List;
import java.util.Optional;

import static io.vertx.openapi.objects.Location.PATH;
import static java.util.stream.Collectors.toList;

public class ParameterImpl implements Parameter {

  private static final String KEY_NAME = "name";
  private static final String KEY_IN = "in";
  private static final String KEY_REQUIRED = "required";
  private static final String KEY_STYLE = "style";
  private static final String KEY_EXPLODE = "explode";

  private final String name;
  private final Location in;
  private final boolean required; // Path parameters MUST be true
  private final boolean explode;
  private final JsonObject parameterModel;
  private Style style;

  public ParameterImpl(String path, JsonObject parameterModel) {
    this.name = parameterModel.getString(KEY_NAME);
    this.required = Optional.ofNullable(parameterModel.getBoolean(KEY_REQUIRED)).orElse(false);
    this.in = Location.parse(parameterModel.getString(KEY_IN));
    if (in == PATH) {
      // if location is "path", name must be part of the path
      if (StringUtil.isNullOrEmpty(name) || !path.contains("{" + name + "}")) {
        throw RouterBuilderException.createInvalidContract("Path parameters MUST have a name that is part of the path");
      }
      // if location is "path", required must be true
      if (!required) {
        throw RouterBuilderException.createInvalidContract("\"required\" MUST be true for path parameters");
      }
    }
    this.style =
      Optional.ofNullable(Style.parse(parameterModel.getString(KEY_STYLE))).orElse(Style.defaultByLocation(in));
    this.explode = Optional.ofNullable(parameterModel.getBoolean(KEY_EXPLODE)).orElse(false);
    this.parameterModel = parameterModel;
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
}
