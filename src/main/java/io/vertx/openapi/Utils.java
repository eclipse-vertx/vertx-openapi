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

package io.vertx.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class Utils {
  public static final JsonArray EMPTY_JSON_ARRAY = new JsonArray(emptyList());
  public static final JsonObject EMPTY_JSON_OBJECT = new JsonObject(emptyMap());

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

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
        try {
          JsonNode node = YAML_MAPPER.readTree(buff.toString());
          return succeededFuture(new JsonObject(node.toString()));
        } catch (JsonProcessingException e) {
          return failedFuture(e);
        }
      } else {
        return failedFuture(new IllegalArgumentException("Only JSON or YAML files are allowed"));
      }
    });
  }
}
