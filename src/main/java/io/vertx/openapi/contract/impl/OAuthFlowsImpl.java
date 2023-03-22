package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.OAuthFlow;
import io.vertx.openapi.contract.OAuthFlows;

public class OAuthFlowsImpl implements OAuthFlows {

  private final JsonObject model;

  private final OAuthFlow implicit;
  private final OAuthFlow password;
  private final OAuthFlow clientCredentials;
  private final OAuthFlow authorizationCode;


  public OAuthFlowsImpl(JsonObject json) {
    this.model = json;

    this.implicit = json.containsKey("implicit") ?
      new OAuthFlowImpl(json.getJsonObject("implicit")) :
      null;
    this.password = json.containsKey("password") ?
      new OAuthFlowImpl(json.getJsonObject("password")) :
      null;
    this.clientCredentials = json.containsKey("clientCredentials") ?
      new OAuthFlowImpl(json.getJsonObject("clientCredentials")) :
      null;
    this.authorizationCode = json.containsKey("authorizationCode") ?
      new OAuthFlowImpl(json.getJsonObject("authorizationCode")) :
      null;
  }

  @Override
  public OAuthFlow getImplicit() {
    return implicit;
  }

  @Override
  public OAuthFlow getPassword() {
    return password;
  }

  @Override
  public OAuthFlow getClientCredentials() {
    return clientCredentials;
  }

  @Override
  public OAuthFlow getAuthorizationCode() {
    return authorizationCode;
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return model;
  }
}
