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
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.impl.Utils.readYamlOrJson;
import static java.util.Collections.emptyMap;

@VertxGen
public interface OpenAPIContract {

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   *
   * @param vertx                  The related Vert.x instance.
   * @param unresolvedContractPath The path to the unresolved contract.
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, String unresolvedContractPath) {
    return readYamlOrJson(vertx, unresolvedContractPath).compose(json -> from(vertx, json));
  }

  /**
   * Resolves / dereferences the passed contract and creates an {@link OpenAPIContract} instance.
   *
   * @param vertx              The related Vert.x instance.
   * @param unresolvedContract The unresolved contract.
   * @return A succeeded {@link Future} holding an {@link OpenAPIContract} instance, otherwise a failed {@link Future}.
   */
  static Future<OpenAPIContract> from(Vertx vertx, JsonObject unresolvedContract) {
    return from(vertx, unresolvedContract, emptyMap());
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

    Map<String, Future<JsonObject>> jsonFilesFuture = new HashMap<>();
    jsonFilesFuture.put(unresolvedContractPath, readYamlOrJson(vertx, unresolvedContractPath));
    additionalContractFiles.forEach((key, value) -> jsonFilesFuture.put(key, readYamlOrJson(vertx, value)));

    return Future.all(new ArrayList<>(jsonFilesFuture.values())).compose(compFut -> {
      Map<String, JsonObject> resolvedFiles = new HashMap<>();
      additionalContractFiles.keySet().forEach(key -> resolvedFiles.put(key, jsonFilesFuture.get(key).result()));
      return from(vertx, jsonFilesFuture.get(unresolvedContractPath).result(), resolvedFiles);
    });
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
    if (unresolvedContract == null) {
      return failedFuture(createInvalidContract("Spec must not be null"));
    }

    OpenAPIVersion version = OpenAPIVersion.fromContract(unresolvedContract);
    String baseUri = "app://";

    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<OpenAPIContract> promise = ctx.promise();

    version.getRepository(vertx, baseUri)
      .compose(repository -> {
      List<Future<?>> validationFutures = new ArrayList<>(additionalContractFiles.size());
      for (String ref : additionalContractFiles.keySet()) {
        // Todo: As soon a more modern Java version is used the validate part could be extracted in a private static
        //  method and reused below.
        JsonObject file = additionalContractFiles.get(ref);
        Future<?> validationFuture = version.validateAdditionalContractFile(vertx, repository, file)
          .compose(v -> vertx.executeBlocking(() -> repository.dereference(ref, JsonSchema.of(ref, file))))
          .transform(ar -> {
            if (ar.failed()) {
              return Future.failedFuture(
                createInvalidContract("Failed to validate additional contract file: " + ref, ar.cause())
              );
            } else {
              return (Future<?>) ar;
            }
          });

        validationFutures.add(validationFuture);
      }
      return Future.all(validationFutures).map(repository);
    }).compose(repository ->
      version.validateContract(vertx, repository, unresolvedContract).compose(res -> {
        try {
          res.checkValidity();
          return version.resolve(vertx, repository, unresolvedContract);
        } catch (JsonSchemaValidationException | UnsupportedOperationException e) {
          return failedFuture(createInvalidContract(null, e));
        }
      })
      .map(resolvedSpec -> (OpenAPIContract) new OpenAPIContractImpl(resolvedSpec, version, repository))
    ).recover(e -> {
      //Convert any non-openapi exceptions into an OpenAPIContractException
      if(e instanceof OpenAPIContractException) {
        return failedFuture(e);
      }

      return failedFuture(createInvalidContract("Found issue in specification for reference: " + e.getMessage(), e));
    }).onComplete(promise);

    return promise.future();
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
