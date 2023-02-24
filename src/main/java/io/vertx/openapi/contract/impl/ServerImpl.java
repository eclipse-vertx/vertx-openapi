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

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.Server;

import java.net.MalformedURLException;
import java.net.URL;

import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;

public class ServerImpl implements Server {

  private static final String KEY_URL = "url";

  private final String basePath;

  private final String url;

  private final JsonObject serverModel;

  public ServerImpl(JsonObject serverModel) {
    this.serverModel = serverModel;
    this.url = serverModel.getString(KEY_URL); // is required should be validated by the contract schema
    if (url.contains("{")) {
      throw createUnsupportedFeature("Server Variables");
    }
    try {
      this.basePath = new URL(url.endsWith("/") ? url.substring(0, url.length() - 1) : url).getPath();
    } catch (MalformedURLException e) {
      throw createInvalidContract("The specified URL is malformed: " + url, e);
    }
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return serverModel.copy();
  }

  @Override
  public String getURL() {
    return url;
  }

  @Override
  public String getBasePath() {
    return basePath;
  }
}
