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

import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.SecurityRequirement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityRequirementImpl implements SecurityRequirement {
  private final JsonObject securityRequirementModel;
  private final Set<String> names;
  private final Map<String, List<String>> scopes;

  public SecurityRequirementImpl(JsonObject securityRequirementModel) {
    this.securityRequirementModel = securityRequirementModel;

    if (securityRequirementModel.isEmpty()) {
      this.names = emptySet();
      this.scopes = emptyMap();
    } else {
      this.names = securityRequirementModel
          .fieldNames()
          .stream()
          .filter(JsonSchema.EXCLUDE_ANNOTATIONS).collect(toUnmodifiableSet());

      this.scopes = unmodifiableMap(
          this.names
              .stream()
              .collect(Collectors.toMap(identity(), name -> extractScopes(securityRequirementModel, name))));
    }
  }

  private static List<String> extractScopes(JsonObject securityRequirementModel, String name) {
    return securityRequirementModel.getJsonArray(name, EMPTY_JSON_ARRAY).stream().map(Object::toString)
        .collect(toList());
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return securityRequirementModel;
  }

  @Override
  public Set<String> getNames() {
    return names;
  }

  @Override
  public List<String> getScopes(String name) {
    if (scopes.containsKey(name)) {
      return scopes.get(name);
    }
    throw new IllegalArgumentException("No security requirement with name " + name);
  }

  @Override
  public boolean isEmpty() {
    return names.isEmpty();
  }

  @Override
  public int size() {
    return names.size();
  }
}
