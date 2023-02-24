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

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.impl.JsonUtil;

import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.openapi.contract.ContractOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.openapi.contract.ContractOptions} original class using Vert.x codegen.
 */
public class ContractOptionsConverter {

  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ContractOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "basePath":
          if (member.getValue() instanceof String) {
            obj.setBasePath((String) member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(ContractOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(ContractOptions obj, java.util.Map<String, Object> json) {
    if (obj.getBasePath() != null) {
      json.put("basePath", obj.getBasePath());
    }
  }
}
