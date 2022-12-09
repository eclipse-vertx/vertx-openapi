package io.vertx.openapi;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class Utils {

  public static final JsonArray EMPTY_JSON_ARRAY = new JsonArray(emptyList());

  public static final JsonObject EMPTY_JSON_OBJECT = new JsonObject(emptyMap());

  private Utils() {

  }
}