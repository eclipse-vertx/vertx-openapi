package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.OAuthFlows;
import io.vertx.openapi.contract.SecurityScheme;

public class SecuritySchemeImpl implements SecurityScheme {

  private final JsonObject model;
  private final String type;
  private final String description;
  private final String name;
  private final String in;
  private final String scheme;
  private final String bearerFormat;
  private final OAuthFlows flows;
  private final String openIdConnectUrl;


  public SecuritySchemeImpl(JsonObject json) {
    this.model = json;

    this.type = json.getString("type");
    this.description = json.getString("description");
    this.name = json.getString("name");
    this.in = json.getString("in");
    this.scheme = json.getString("scheme");
    this.bearerFormat = json.getString("bearerFormat");
    this.flows = json.containsKey("flows") ?
      new OAuthFlowsImpl(json.getJsonObject("flows")) :
      null;
    this.openIdConnectUrl = json.getString("openIdConnectUrl");

  }

  @Override
  public JsonObject getOpenAPIModel() {
    return model;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getIn() {
    return in;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public String getBearerFormat() {
    return bearerFormat;
  }

  @Override
  public OAuthFlows getFlows() {
    return flows;
  }

  @Override
  public String getOpenIdConnectUrl() {
    return openIdConnectUrl;
  }
}
