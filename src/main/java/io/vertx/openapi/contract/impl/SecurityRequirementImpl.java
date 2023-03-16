package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.SecurityRequirement;

import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;

public class SecurityRequirementImpl implements SecurityRequirement {

  private final JsonObject securityRequirementModel;
  private final Set<String> names;
  private final Map<String, List<String>> scopes;
  private final boolean empty;
  private final int size;

  SecurityRequirementImpl(JsonObject securityRequirementModel) {
    this.securityRequirementModel = securityRequirementModel;
    this.empty = securityRequirementModel.isEmpty();
    this.size = securityRequirementModel.size();

    if (!securityRequirementModel.isEmpty()) {
      this.names = unmodifiableSet(securityRequirementModel.fieldNames());
      this.scopes = new HashMap<>();
      for (String name : this.names) {
        this.scopes.put(name, unmodifiableList(securityRequirementModel.getJsonArray(name).stream()
          .map(Object::toString)
          .collect(toList())));
      }
    } else {
      this.names = null;
      this.scopes = null;
    }
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
    return empty;
  }

  @Override
  public int size() {
    return size;
  }
}
