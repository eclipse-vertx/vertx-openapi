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

import static io.vertx.json.schema.common.dsl.SchemaType.OBJECT;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.impl.ParameterImpl.parseParameters;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.RequestBody;
import io.vertx.openapi.contract.Response;
import io.vertx.openapi.contract.SecurityRequirement;
import io.vertx.openapi.validation.analyser.ContentAnalyserFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class OperationImpl implements Operation {
  private static final Logger LOG = LoggerFactory.getLogger(OperationImpl.class);

  private static final Pattern RESPONSE_CODE_PATTERN = Pattern.compile("\\d\\d\\d");

  private static final String KEY_OPERATION_ID = "operationId";
  private static final String KEY_TAGS = "tags";
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_REQUEST_BODY = "requestBody";
  private static final String KEY_RESPONSES = "responses";
  private static final String KEY_SECURITY = "security";

  private final String operationId;
  private final String path;
  private final HttpMethod method;
  private final JsonObject operationModel;
  private final List<Parameter> parameters;
  private final RequestBody requestBody;
  private final List<String> tags;
  private final Response defaultResponse;
  private final Map<Integer, Response> responses;
  private final String absolutePath;
  private final List<SecurityRequirement> securityRequirements;
  private final Map<String, Object> extensions;

  public OperationImpl(String absolutePath, String path, HttpMethod method, JsonObject operationModel,
      List<Parameter> pathParameters, Map<String, Object> pathExtensions,
      List<SecurityRequirement> globalSecReq, Map<String, ContentAnalyserFactory> additionalMediaTypes) {
    this.absolutePath = absolutePath;
    this.operationId = operationModel.getString(KEY_OPERATION_ID);
    this.method = method;
    this.path = path;
    this.operationModel = operationModel;

    HashMap<String, Object> allExtensions = new HashMap<>(Operation.super.getExtensions());
    pathExtensions.forEach(allExtensions::putIfAbsent);
    this.extensions = unmodifiableMap(allExtensions);

    this.tags =
        operationModel.getJsonArray(KEY_TAGS, EMPTY_JSON_ARRAY).stream().map(Object::toString)
            .collect(toUnmodifiableList());

    this.securityRequirements =
        operationModel.containsKey(KEY_SECURITY) ? operationModel.getJsonArray(KEY_SECURITY).stream()
            .map(JsonObject.class::cast)
            .map(SecurityRequirementImpl::new)
            .collect(toUnmodifiableList()) : globalSecReq;

    List<Parameter> operationParameters =
        parseParameters(path, operationModel.getJsonArray(KEY_PARAMETERS, EMPTY_JSON_ARRAY));
    // pretty sure there is a smarter / more efficient way
    for (Parameter pathParam : pathParameters) {
      Optional<Parameter> parameterDuplicate = operationParameters.stream()
          .filter(param -> pathParam.getName().equals(param.getName()) && pathParam.getIn().equals(param.getIn()))
          .findAny();

      if (parameterDuplicate.isPresent()) {
        LOG.debug("Found ambiguous parameter (" + pathParam.getName() + ") in operation: " + operationId);
      } else {
        operationParameters.add(pathParam);
      }
    }

    long explodedQueryParams =
        operationParameters.stream()
            .filter(p -> p.isExplode() && p.getStyle() == FORM && p.getIn() == QUERY && p.getSchemaType() == OBJECT)
            .count();
    if (explodedQueryParams > 1) {
      String msg =
          "Found multiple exploded query parameters of style form with type object in operation: " + operationId;
      throw createInvalidContract(msg);
    }

    this.parameters = unmodifiableList(operationParameters);

    JsonObject requestBodyJson = operationModel.getJsonObject(KEY_REQUEST_BODY);
    if (requestBodyJson == null || requestBodyJson.isEmpty()) {
      this.requestBody = null;
    } else {
      this.requestBody = new RequestBodyImpl(requestBodyJson, operationId, additionalMediaTypes);
    }

    JsonObject responsesJson = operationModel.getJsonObject(KEY_RESPONSES, EMPTY_JSON_OBJECT);
    if (responsesJson.isEmpty()) {
      String msg = "No responses were found in operation: " + operationId;
      throw createInvalidContract(msg);
    }
    defaultResponse = responsesJson.stream().filter(entry -> "default".equalsIgnoreCase(entry.getKey())).findFirst()
        .map(entry ->
            new ResponseImpl((JsonObject) entry.getValue(), operationId, additionalMediaTypes)).orElse(null);
    responses =
        unmodifiableMap(
            responsesJson
                .fieldNames()
                .stream()
                .filter(JsonSchema.EXCLUDE_ANNOTATIONS)
                .filter(RESPONSE_CODE_PATTERN.asPredicate())
                .collect(
                    toMap(
                        Integer::parseInt,
                        key -> new ResponseImpl(responsesJson.getJsonObject(key), operationId, additionalMediaTypes))));
  }

  @Override
  public String getOperationId() {
    return operationId;
  }

  @Override
  public JsonObject getOpenAPIModel() {
    return operationModel;
  }

  @Override
  public HttpMethod getHttpMethod() {
    return method;
  }

  @Override
  public String getOpenAPIPath() {
    return path;
  }

  @Override
  public String getAbsoluteOpenAPIPath() {
    return absolutePath;
  }

  @Override
  public List<String> getTags() {
    return tags;
  }

  @Override
  public List<Parameter> getParameters() {
    return parameters;
  }

  @Override
  public RequestBody getRequestBody() {
    return requestBody;
  }

  @Override
  public Response getDefaultResponse() {
    return defaultResponse;
  }

  @Override
  public Response getResponse(int responseCode) {
    return responses.get(responseCode);
  }

  @Override
  public List<SecurityRequirement> getSecurityRequirements() {
    return securityRequirements;
  }

  @Override
  public Map<String, Object> getExtensions() {
    return extensions;
  }
}
