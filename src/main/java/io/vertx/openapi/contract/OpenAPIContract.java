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

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.mediatype.MediaTypeRegistry;

import java.util.List;
import java.util.Map;

@VertxGen
public interface OpenAPIContract {


  static OpenAPIContractBuilder builder(Vertx vertx) {
    return new OpenAPIContractBuilder(vertx);
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   *
   * @param vertx                  The related Vert.x instance.
   * @param unresolvedContractPath The path to the unresolved contract.
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, String unresolvedContractPath) {
    return builder(vertx).contract(unresolvedContractPath).build();
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   *
   * @param vertx              The related Vert.x instance.
   * @param unresolvedContract The unresolved contract.
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, JsonObject unresolvedContract) {
    if (unresolvedContract == null)
      return Future.failedFuture(OpenAPIContractException.createInvalidContract("Spec must not be null"));
    return builder(vertx)
      .contract(unresolvedContract)
      .build();
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   * <p>
   * This method can be used in case that the contract is split into several files. These files can be passed in a
   * Map that has the reference as key and the path to the file as value.
   *
   * @param vertx                   The related Vert.x instance.
   * @param unresolvedContractPath  The path to the unresolved contract.
   * @param additionalContractFiles The additional contract files
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, String unresolvedContractPath,
                                      Map<String, String> additionalContractFiles) {

    return builder(vertx)
      .contract(unresolvedContractPath)
      .addAdditionalContentFiles(additionalContractFiles)
      .build();
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   * <p>
   * This method can be used in case that the contract is split into several files. These files can be passed in a
   * Map that has the reference as key and the path to the file as value.
   *
   * @param vertx                   The related Vert.x instance.
   * @param unresolvedContract      The unresolved contract.
   * @param additionalContractFiles The additional contract files
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, JsonObject unresolvedContract,
                                      Map<String, JsonObject> additionalContractFiles) {
    if (unresolvedContract == null)
      return Future.failedFuture(OpenAPIContractException.createInvalidContract("Spec must not be null"));
    return builder(vertx)
      .contract(unresolvedContract)
      .addAdditionalContent(additionalContractFiles)
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

  /**
   * Gets the mediatype registry.
   *
   * @return The registry.
   */
  @GenIgnore
  MediaTypeRegistry mediaTypes();
}
