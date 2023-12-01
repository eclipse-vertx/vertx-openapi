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

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Path;
import io.vertx.openapi.contract.SecurityRequirement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.HttpMethod.OPTIONS;
import static io.vertx.core.http.HttpMethod.PATCH;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static io.vertx.core.http.HttpMethod.TRACE;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.impl.ParameterImpl.parseParameters;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static java.util.Collections.unmodifiableList;

public class PathImpl implements Path {
  // VisibleForTesting
  static final Pattern INVALID_CURLY_BRACES = Pattern.compile("/[^/]+\\{|}[^/]+/|}[^/]+$");
  private static final String KEY_PARAMETERS = "parameters";
  private static final Map<String, HttpMethod> SUPPORTED_METHODS;

  static {
    SUPPORTED_METHODS = new HashMap<>(8);
    SUPPORTED_METHODS.put("get", GET);
    SUPPORTED_METHODS.put("put", PUT);
    SUPPORTED_METHODS.put("post", POST);
    SUPPORTED_METHODS.put("delete", DELETE);
    SUPPORTED_METHODS.put("options", OPTIONS);
    SUPPORTED_METHODS.put("head", HEAD);
    SUPPORTED_METHODS.put("patch", PATCH);
    SUPPORTED_METHODS.put("trace", TRACE);
  }

  private final String name;
  private final List<Operation> operations;
  private final List<Parameter> parameters;
  private final JsonObject pathModel;
  private final String absolutePath;

  public PathImpl(String basePath, String name, JsonObject pathModel, List<SecurityRequirement> globalSecReq) {
    this.absolutePath = (basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath) + name;
    this.pathModel = pathModel;
    if (name.contains("*")) {
      throw createInvalidContract("Paths must not have a wildcard (asterisk): " + name);
    }
    if (INVALID_CURLY_BRACES.matcher(name).find()) {
      throw createInvalidContract(
        "Curly brace MUST be the first/last character in a path segment (/{parameterName}/): " + name);
    }
    if (name.length() > 1 && name.endsWith("/")) {
      this.name = name.substring(0, name.length() - 1);
    } else {
      this.name = name;
    }
    this.parameters = unmodifiableList(parseParameters(name, pathModel.getJsonArray(KEY_PARAMETERS, EMPTY_JSON_ARRAY)));

    List<Operation> ops = new ArrayList<>();
    SUPPORTED_METHODS.forEach((methodName, method) -> Optional.ofNullable(pathModel.getJsonObject(methodName))
      .map(operationModel -> new OperationImpl(absolutePath, name, method, operationModel, parameters,
        getExtensions(), globalSecReq))
      .ifPresent(ops::add));
    this.operations = unmodifiableList(ops);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Operation> getOperations() {
    return operations;
  }

  @Override
  public List<Parameter> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return pathModel;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }
}
