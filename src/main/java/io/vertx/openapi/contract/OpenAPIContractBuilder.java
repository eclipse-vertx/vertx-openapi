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
  private String contractFile;
  private JsonObject contract;
  private final Map<String, String> additionalContractFiles = new HashMap<>();
  private final Map<String, JsonObject> additionalContracts = new HashMap<>();

  public OpenAPIContractBuilder(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Sets the path to the contract file. Either provide the path to the contract or the parsed contract,
   * not both. Overrides the contract set by {@link #setContract(JsonObject)}.
   *
   * @param contractPath The path to the contract file
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder setContract(String contractPath) {
    this.contractFile = contractPath;
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
    this.contractFile = null;
    return this;
  }

  /**
   * Puts a contract that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced contracts. Overrides a previously
   * added contract, when the same key is used.
   *
   * @param key  The unique key for the contract.
   * @param path The path to the contract file.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder putAdditionalContractFile(String key, String path) {
    additionalContractFiles.put(key, path);
    additionalContracts.remove(key);
    return this;
  }

  /**
   * Uses the contract files from the provided map to resolve referenced contracts.
   * Replaces all previously put contracts by {@link #putAdditionalContractFile(String, String)}.
   * If the same key is used also overrides the contracts set by {@link #putAdditionalContract(String, JsonObject)}
   * and {@link #setAdditionalContracts(Map)}.
   *
   * @param contractFiles A map that contains all additional contract files.
   * @return The builder, for a fluent interface.
   */
  public OpenAPIContractBuilder setAdditionalContractFiles(Map<String, String> contractFiles) {
    additionalContractFiles.clear();
    for (var e : contractFiles.entrySet()) {
      putAdditionalContractFile(e.getKey(), e.getValue());
      additionalContracts.remove(e.getKey());
    }
    return this;
  }

  /**
   * Adds a contract that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced contracts.
   *
   * @param key     The unique key for the contract.
   * @param content The parsed contract.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder putAdditionalContract(String key, JsonObject content) {
    additionalContracts.put(key, content);
    additionalContractFiles.remove(key);
    return this;
  }

  /**
   * Uses the contracts from the provided map to resolve referenced contracts.
   * Replaces all previously put contracts by {@link #putAdditionalContract(String, JsonObject)}.
   * If the same key is used also replaces the contracts set by {@link #putAdditionalContractFile(String, String)}
   * and {@link #setAdditionalContractFiles(Map)}.
   *
   * @param contracts A map that contains all additional contract files.
   * @return The builder, for a fluent interface.
   */
  public OpenAPIContractBuilder setAdditionalContracts(Map<String, JsonObject> contracts) {
    additionalContracts.clear();
    for (var e : contracts.entrySet()) {
      putAdditionalContract(e.getKey(), e.getValue());
      additionalContractFiles.remove(e.getKey());
    }
    return this;
  }

  /**
   * Builds the contract.
   *
   * @return The contract.
   */
  public Future<OpenAPIContract> build() {
    if (contractFile == null && contract == null) {
      return Future.failedFuture(new OpenAPIContractBuilderException(
          "Neither a contract file or a contract is set. One of them must be set."));
    }

    Future<JsonObject> readContract = contractFile == null
        ? Future.succeededFuture(contract)
        : Utils.readYamlOrJson(vertx, contractFile);

    var resolvedContracts = Future
      .succeededFuture(additionalContracts)
      .compose(x -> readContractFiles()
        .map(r -> {
          var all = new HashMap<>(x);
          all.putAll(r);
          return all;
        }));

    return Future.all(readContract, resolvedContracts)
      .compose(x -> {
        JsonObject contract = x.resultAt(0);
        Map<String, JsonObject> other = x.resultAt(1);
        return buildOpenAPIContract(contract, other);
      });
  }

  private Future<OpenAPIContract> buildOpenAPIContract(JsonObject unresolvedContract,
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
            // method and reused below.
            JsonObject file = additionalContractFiles.get(ref);
            Future<?> validationFuture = version.validateAdditionalContractFile(vertx, repository, file)
                .compose(v -> vertx.executeBlocking(() -> repository.dereference(ref, JsonSchema.of(ref, file))));

            validationFutures.add(validationFuture);
          }
          return Future.all(validationFutures).map(repository);
        }).compose(repository -> version.validateContract(vertx, repository, unresolvedContract).compose(res -> {
          try {
            res.checkValidity();
            return version.resolve(vertx, repository, unresolvedContract);
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

  private Future<Map<String, JsonObject>> readContractFiles() {
    if (additionalContractFiles.isEmpty()) return Future.succeededFuture(Map.of());

    var read = new HashMap<String, JsonObject>();
    return Future.all(additionalContractFiles.entrySet().stream()
        .map(e -> Utils.readYamlOrJson(vertx, e.getValue())
            .map(c -> read.put(e.getKey(), c)))
        .collect(Collectors.toList()))
        .map(ign -> read);
  }

}
