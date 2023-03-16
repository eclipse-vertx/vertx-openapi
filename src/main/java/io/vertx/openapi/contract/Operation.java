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

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import java.util.List;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#operation-Object">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#operation-Object">Operation V3.0</a>
 */
@VertxGen
public interface Operation extends OpenAPIObject {

  /**
   * @return operationId of this operation
   */
  String getOperationId();

  /**
   * @return http method of this operation
   */
  HttpMethod getHttpMethod();

  /**
   * @return path in OpenAPI style
   */
  String getOpenAPIPath();

  /**
   * @return absolute path in OpenAPI style
   */
  String getAbsoluteOpenAPIPath();

  /**
   * @return tags of this operation
   */
  List<String> getTags();

  /**
   * @return parameters of this operation
   */
  List<Parameter> getParameters();

  /**
   * @return request body of the operation, or null if no request body is defined
   */
  RequestBody getRequestBody();

  /**
   * @return the default response, or null if no default response is defined.
   */
  @Nullable
  Response getDefaultResponse();

  /**
   * Returns the response to the passed response code or null.
   *
   * @param responseCode The related response code
   * @return The related response, or null.
   */
  Response getResponse(int responseCode);

  /**
   * Returns the applicable list of security requirements (scopes) or empty list.
   * @return The related security requirement.
   */
  @Nullable
  List<SecurityRequirement> getSecurityRequirements();
}
