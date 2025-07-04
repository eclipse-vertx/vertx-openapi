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
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.SchemaRepository;
import java.util.List;
import java.util.Map;

@VertxGen
public interface OpenAPIContract {

  /**
   * Instantiates a new builder for an openapi-contract.
   *
   * @param vertx The vert.x instance
   * @return A new builder.
   */
  static OpenAPIContractBuilder builder(Vertx vertx) {
    return new OpenAPIContractBuilder(vertx);
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   *
   * @param vertx        The related Vert.x instance.
   * @param contractPath The path to the contract.
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, String contractPath) {
    return builder(vertx).setContractPath(contractPath).build();
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   *
   * @param vertx    The related Vert.x instance.
   * @param contract The contract.
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, JsonObject contract) {
    return builder(vertx)
        .setContract(contract)
        .build();
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   * <p>
   * This method can be used in case that the contract is split into several parts. These parts can be passed in a
   * Map that has the reference as key and the path to the part as value.
   *
   * @param vertx                       The related Vert.x instance.
   * @param contractPath                The path to the contract.
   * @param additionalContractPartPaths The additional contract part paths
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, String contractPath,
      Map<String, String> additionalContractPartPaths) {

    return builder(vertx)
        .setContractPath(contractPath)
        .setAdditionalContractPartPaths(additionalContractPartPaths)
        .build();
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   * <p>
   * This method can be used in case that the contract is split into several parts. These parts can be passed in a
   * Map that has the reference as key and the part as value.
   *
   * @param vertx                   The related Vert.x instance.
   * @param contract                The unresolved contract.
   * @param additionalContractParts The additional contract parts
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, JsonObject contract,
      Map<String, JsonObject> additionalContractParts) {
    return builder(vertx)
        .setContract(contract)
        .setAdditionalContractParts(additionalContractParts)
        .build();

  }

  /**
   * Access to an operation defined in the contract with {@code operationId}.
   *
   * @param operationId the id of the operation.
   * @return the requested operation.
   * @throws IllegalArgumentException if the operation id doesn't exist in the contract.
   */
  @Nullable
  Operation operation(String operationId);

  /**
   * @return all operations defined in the contract.
   */
  List<Operation> operations();

  /**
   * @return all {@link Path Paths} defined in the OpenAPI contract.
   */
  List<Path> getPaths();

  /**
   * @return the resolved OpenAPI contract as {@link JsonObject}.
   */
  JsonObject getRawContract();

  /**
   * @return the OpenAPI version of the contract.
   */
  OpenAPIVersion getVersion();

  /**
   * @return the {@link SchemaRepository} to validate against.
   */
  SchemaRepository getSchemaRepository();

  /**
   * @return the servers of the contract.
   */
  List<Server> getServers();

  /**
   * Finds the related {@link Path} object based on the passed url path.
   *
   * @param urlPath The path of the request.
   * @return the found {@link Path} object, or null if the passed path doesn't match any {@link Path} object.
   */
  @Nullable
  Path findPath(String urlPath);

  /**
   * Finds the related {@link Operation} object based on the passed url path and method.
   *
   * @param urlPath The path of the request.
   * @param method  The method of the request.
   * @return the found {@link Operation} object, or null if the passed path and method doesn't match any
   * {@link Operation} object.
   */
  @Nullable
  Operation findOperation(String urlPath, HttpMethod method);

  /**
   * Returns the applicable list of global security requirements (scopes) or empty list.
   *
   * @return The related security requirement.
   */
  List<SecurityRequirement> getSecurityRequirements();

  /**
   * Gets the related {@link SecurityScheme} object based on the passed name.
   *
   * @param name The name of the security scheme.
   * @return the found {@link SecurityScheme} object, or null if the passed path and method doesn't match any
   * {@link Operation} object.
   */
  @Nullable
  SecurityScheme securityScheme(String name);
}
