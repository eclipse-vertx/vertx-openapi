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

package io.vertx.openapi.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class Utils {
  public static final JsonArray EMPTY_JSON_ARRAY = new JsonArray(emptyList());
  public static final JsonObject EMPTY_JSON_OBJECT = new JsonObject(emptyMap());

  private Utils() {

  }

  /**
   * Reads a JSON or YAML file from the passed path and transforms it into a JsonObject.
   *
   * @param vertx The related Vert.x instance
   * @param path  The path to the YAML or JSON file
   * @return A succeeded Future holding the JsonObject, or a failed Future if the file could not be parsed.
   */
  public static Future<JsonObject> readYamlOrJson(Vertx vertx, String path) {
    return vertx.fileSystem().readFile(path).compose(buff -> {
      String suffix = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
      if ("json".equals(suffix)) {
        return succeededFuture(buff.toJsonObject());
      } else if ("yaml".equals(suffix) || "yml".equals(suffix)) {
        return yamlStringToJson((buff.toString(StandardCharsets.UTF_8));
      } else {
        return failedFuture(new IllegalArgumentException("Only JSON or YAML files are allowed"));
      }
    });
  }

    /**
   * Reads YAML string and transforms it into a JsonObject.
   *
   * @param path  The yamlString proper YAML formatted STring
   * @return A succeeded Future holding the JsonObject, or a failed Future if the file could not be parsed.
   */
  public static Future<JsonObject> yamlStringToJson(String yamlString) {
    try {
      final Yaml yaml = new Yaml(new OpenAPIYamlConstructor());
      Map<Object, Object> doc = yaml.load(yamlString);
      return succeededFuture(new JsonObject(jsonify(doc)));
    } catch (RuntimeException e) {
      return failedFuture(e);
    }
  }

  /**
   * Yaml allows map keys of type object, however json always requires key as String,
   * this helper method will ensure we adapt keys to the right type
   *
   * @param yaml yaml map
   * @return json map
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> jsonify(Map<Object, Object> yaml) {
    final Map<String, Object> json = new LinkedHashMap<>();

    for (Map.Entry<Object, Object> kv : yaml.entrySet()) {
      Object value = kv.getValue();
      if (value instanceof Map) {
        value = jsonify((Map<Object, Object>) value);
      }
      json.put(kv.getKey().toString(), value);
    }

    return json;
  }
}
