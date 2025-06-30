/*
 * Copyright (c) 2025, Lukas Jelonek
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

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;
import io.vertx.openapi.impl.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@GenIgnore
public class OpenAPIContractBuilder {

  public static class OpenAPIContractBuilderException extends RuntimeException {
    public OpenAPIContractBuilderException(String message) {
      super(message);
    }
  }

  private final Vertx vertx;
  private String contractPath;
  private JsonObject contract;
  private final Map<String, String> additionalContractPaths = new HashMap<>();
  private final Map<String, JsonObject> additionalContractParts = new HashMap<>();

  public OpenAPIContractBuilder(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Sets the path to the contract. Either provide the path to the contract or the parsed contract,
   * not both. Overrides the contract set by {@link #setContract(JsonObject)}.
   *
   * @param contractPath The path to the contract
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder setContract(String contractPath) {
    this.contractPath = contractPath;
    this.contract = null;
    return this;
  }

  /**
   * Sets the contract. Either provide the contract or the path to the contract,
   * not both. Overrides the contract set by {@link #setContract(String)}.
   *
   * @param contract The parsed contract
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder setContract(JsonObject contract) {
    this.contract = contract;
    this.contractPath = null;
    return this;
  }

  /**
   * Puts a contract that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced contracts. Overrides a previously
   * added contract when the same key is used.
   *
   * @param key  The unique key for the contract.
   * @param path The path to the contract.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder putAdditionalContractPath(String key, String path) {
    additionalContractPaths.put(key, path);
    additionalContractParts.remove(key);
    return this;
  }

  /**
   * Uses the contract paths from the provided map to resolve referenced contracts.
   * Replaces all previously put contracts by {@link #putAdditionalContractPath(String, String)}.
   * If the same key is used also overrides the contracts set by {@link #putAdditionalContractPart(String, JsonObject)}
   * and {@link #setAdditionalContractParts(Map)}.
   *
   * @param contractPaths A map that contains all additional contract paths.
   * @return The builder, for a fluent interface.
   */
  public OpenAPIContractBuilder setAdditionalContractPaths(Map<String, String> contractPaths) {
    additionalContractPaths.clear();
    for (var e : contractPaths.entrySet()) {
      putAdditionalContractPath(e.getKey(), e.getValue());
      additionalContractParts.remove(e.getKey());
    }
    return this;
  }

  /**
   * Puts a contract that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced contracts.
   *
   * @param key          The unique key for the contract.
   * @param contractPart The contract object.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder putAdditionalContractPart(String key, JsonObject contractPart) {
    additionalContractParts.put(key, contractPart);
    additionalContractPaths.remove(key);
    return this;
  }

  /**
   * Uses the contracts from the provided map to resolve referenced contracts.
   * Replaces all previously put contracts by {@link #putAdditionalContractPart(String, JsonObject)}.
   * If the same key is used also replaces the contracts set by {@link #putAdditionalContractPath(String, String)}
   * and {@link #setAdditionalContractPaths(Map)}.
   *
   * @param contractParts A map that contains additional contract parts.
   * @return The builder, for a fluent interface.
   */
  public OpenAPIContractBuilder setAdditionalContractParts(Map<String, JsonObject> contractParts) {
    additionalContractParts.clear();
    for (var e : contractParts.entrySet()) {
      putAdditionalContractPart(e.getKey(), e.getValue());
      additionalContractPaths.remove(e.getKey());
    }
    return this;
  }

  /**
   * Builds the contract.
   *
   * @return The contract.
   */
  public Future<OpenAPIContract> build() {

    if (contractPath == null && contract == null) {
      return Future.failedFuture(new OpenAPIContractBuilderException(
          "Neither a contract path or a contract is set. One of them must be set."));
    }

    Future<JsonObject> readContract = contractPath == null
        ? Future.succeededFuture(contract)
        : Utils.readYamlOrJson(vertx, contractPath);

    var resolvedContractParts = readContractPaths()
        .map(r -> {
          var all = new HashMap<>(additionalContractParts);
          all.putAll(r);
          return all;
        });

    return Future.all(readContract, resolvedContractParts)
        .compose(composite -> {
          JsonObject contract = composite.resultAt(0);
          Map<String, JsonObject> contractParts = composite.resultAt(1);
          return buildOpenAPIContract(contract, contractParts);
        });
  }

  private Future<OpenAPIContract> buildOpenAPIContract(JsonObject resolvedContract,
      Map<String, JsonObject> additionalContractParts) {
    OpenAPIVersion version = OpenAPIVersion.fromContract(resolvedContract);
    String baseUri = "app://";

    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<OpenAPIContract> promise = ctx.promise();

    version.getRepository(vertx, baseUri)

        .compose(repository -> {
          List<Future<?>> validationFutures = new ArrayList<>(additionalContractParts.size());
          for (String ref : additionalContractParts.keySet()) {
            // Todo: As soon a more modern Java version is used the validate part could be extracted in a private static
            // method and reused below.
            JsonObject file = additionalContractParts.get(ref);
            Future<?> validationFuture = version.validateAdditionalContractFile(vertx, repository, file)
                .compose(v -> vertx.executeBlocking(() -> repository.dereference(ref, JsonSchema.of(ref, file))));

            validationFutures.add(validationFuture);
          }
          return Future.all(validationFutures).map(repository);
        }).compose(repository -> version.validateContract(vertx, repository, resolvedContract).compose(res -> {
          try {
            res.checkValidity();
            return version.resolve(vertx, repository, resolvedContract);
          } catch (JsonSchemaValidationException | UnsupportedOperationException e) {
            return failedFuture(createInvalidContract(null, e));
          }
        })
            .map(resolvedSpec -> new OpenAPIContractImpl(resolvedSpec, version, repository)))
        .recover(e -> {
          // Convert any non-openapi exceptions into an OpenAPIContractException
          if (e instanceof OpenAPIContractException) {
            return failedFuture(e);
          }

          return failedFuture(
              createInvalidContract("Found issue in specification for reference: " + e.getMessage(), e));
        }).onComplete(promise);

    return promise.future();
  }

  private Future<Map<String, JsonObject>> readContractPaths() {
    if (additionalContractPaths.isEmpty())
      return Future.succeededFuture(Map.of());

    var read = new HashMap<String, JsonObject>();
    return Future.all(additionalContractPaths.entrySet().stream()
        .map(e -> Utils.readYamlOrJson(vertx, e.getValue())
            .map(c -> read.put(e.getKey(), c)))
        .collect(Collectors.toList()))
        .map(ign -> read);
  }

}
