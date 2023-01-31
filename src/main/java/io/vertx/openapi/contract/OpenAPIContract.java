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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.Utils.readYamlOrJson;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
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
    return readYamlOrJson(vertx, unresolvedContractPath).compose(path -> from(vertx, path, emptyMap()));
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

    return CompositeFuture.all(new ArrayList<>(jsonFilesFuture.values())).compose(compFut -> {
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

    version.getRepository(vertx, baseUri).compose(repository -> {
      List<Future> validationFutures = new ArrayList<>(additionalContractFiles.size());
      for (String ref : additionalContractFiles.keySet()) {
        // Todo: As soon a more modern Java version is used the validate part could be extracted in a private static
        //  method and reused below.
        Future<?> validationFuture = version.validate(vertx, repository, additionalContractFiles.get(ref)).map(res -> {
          if (Boolean.FALSE.equals(res.getValid())) {
            String msg = "Found issue in specification for reference: " + ref;
            throw createInvalidContract(msg, res.toException(""));
          } else {
            return repository.dereference(ref, JsonSchema.of(ref, additionalContractFiles.get(ref)));
          }
        });
        validationFutures.add(validationFuture);
      }
      return CompositeFuture.all(validationFutures).map(repository);
    }).compose(repository ->
      version.validate(vertx, repository, unresolvedContract).compose(res -> {
        if (Boolean.FALSE.equals(res.getValid())) {
          return failedFuture(createInvalidContract(null, res.toException("")));
        } else {
          return version.resolve(vertx, repository, unresolvedContract);
        }
      }).map(resolvedSpec -> (OpenAPIContract) new OpenAPIContractImpl(resolvedSpec, version, repository))
    ).onComplete(promise);

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
   * Finds the related {@link Path} object based on the passed url path.
   *
   * @param urlPath The path of the request.
   * @return the found {@link Path} object, or null if the passed path doesn't match any {@link Path} object.
   */
  Path findPath(String urlPath);

  /**
   * Finds the related {@link Operation} object based on the passed url path and method.
   *
   * @param urlPath The path of the request.
   * @param method  The method of the request.
   * @return the found {@link Operation} object, or null if the passed path and method doesn't match any {@link Operation} object.
   */
  Operation findOperation(String urlPath, HttpMethod method);
}
