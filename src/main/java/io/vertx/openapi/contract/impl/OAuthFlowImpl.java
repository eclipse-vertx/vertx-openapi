package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.OAuthFlow;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableSet;

public class OAuthFlowImpl implements OAuthFlow {

  private final JsonObject model;

  private final String authorizationUrl;
  private final String tokenUrl;
  private final String refreshUrl;
  private final Set<String> scopes;


  public OAuthFlowImpl(JsonObject json) {
    this.model = json;
    this.authorizationUrl = json.getString("authorizationUrl");
    this.tokenUrl = json.getString("tokenUrl");
    this.refreshUrl = json.getString("refreshUrl");
    this.scopes = json.containsKey("scopes") ?
      unmodifiableSet(
        json
          .getJsonObject("scopes")
          .fieldNames()
          .stream()
          .filter(JsonSchema.EXCLUDE_ANNOTATIONS)
          .collect(Collectors.toSet()))
      :
      Collections.emptySet();
  }

  @Override
  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  @Override
  public String getTokenUrl() {
    return tokenUrl;
  }

  @Override
  public String getRefreshUrl() {
    return refreshUrl;
  }

  @Override
  public Set<String> getScopes() {
    return scopes;
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return model;
  }
}
