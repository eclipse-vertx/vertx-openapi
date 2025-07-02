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
import static io.vertx.core.Future.succeededFuture;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder for OpenAPIContracts.<br>
 * <p>
 * In the simplest case (you only have one contract) you must either provide a path to your openapi-contract in json
 * or yaml format or an already parsed openapi-spec as a {@link JsonObject}.
 * See {@link OpenAPIContractBuilder#setContractPath(String)} and {@link OpenAPIContractBuilder#setContract(JsonObject)}.
 * <br>
 * If your contract is split across different files you must load the main contract as described above and additionally
 * provide the referenced contract parts. See {@link OpenAPIContractBuilder#putAdditionalContractPartPath(String, String)},
 * {@link OpenAPIContractBuilder#setAdditionalContractPartPaths(Map)},
 * {@link OpenAPIContractBuilder#putAdditionalContractPart(String, JsonObject)},
 * {@link OpenAPIContractBuilder#setAdditionalContractParts(Map)}.
 * <br>
 */
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
  private final Map<String, String> additionalContractPartPaths = new HashMap<>();
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
  public OpenAPIContractBuilder setContractPath(String contractPath) {
    this.contractPath = contractPath;
    this.contract = null;
    return this;
  }

  /**
   * Sets the contract. Either provide the contract or the path to the contract,
   * not both. Overrides the contract set by {@link #setContractPath(String)}.
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
   * Puts an additional contract part path that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced additional contract parts. Overrides a previously
   * added additional contract part when the same reference is used.
   *
   * @param ref  The unique reference of the additional contract part.
   * @param path The path to the contract part.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder putAdditionalContractPartPath(String ref, String path) {
    additionalContractPartPaths.put(ref, path);
    additionalContractParts.remove(ref);
    return this;
  }

  /**
   * Uses the additional contract part paths from the provided map to resolve referenced contract parts.
   * Replaces all previously put additional contract part paths by {@link #putAdditionalContractPartPath(String, String)}.
   * If the same reference is used it overrides the additional contract part added by {@link #putAdditionalContractPart(String, JsonObject)}
   * or {@link #setAdditionalContractParts(Map)}.
   *
   * @param contractPartPaths A map that contains all additional contract part paths.
   * @return The builder, for a fluent interface.
   */
  public OpenAPIContractBuilder setAdditionalContractPartPaths(Map<String, String> contractPartPaths) {
    additionalContractPartPaths.clear();
    for (var e : contractPartPaths.entrySet()) {
      putAdditionalContractPartPath(e.getKey(), e.getValue());
      additionalContractParts.remove(e.getKey());
    }
    return this;
  }

  /**
   * Puts an additional contract part that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced additional contract parts.
   *
   * @param ref          The unique reference of the additional contract part.
   * @param contractPart The additional contract part.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder putAdditionalContractPart(String ref, JsonObject contractPart) {
    additionalContractParts.put(ref, contractPart);
    additionalContractPartPaths.remove(ref);
    return this;
  }

  /**
   * Uses the additional contract parts from the provided map to resolve referenced additional contract parts.
   * Replaces all previously put additional contract parts by {@link #putAdditionalContractPart(String, JsonObject)}.
   * If the same reference is used also replaces the additional contract part paths added by {@link #putAdditionalContractPartPath(String, String)}
   * or {@link #setAdditionalContractPartPaths(Map)}.
   *
   * @param contractParts A map that contains additional contract parts.
   * @return The builder, for a fluent interface.
   */
  public OpenAPIContractBuilder setAdditionalContractParts(Map<String, JsonObject> contractParts) {
    additionalContractParts.clear();
    for (var e : contractParts.entrySet()) {
      putAdditionalContractPart(e.getKey(), e.getValue());
      additionalContractPartPaths.remove(e.getKey());
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
          "Neither a contract path nor a contract is set. One of them must be set."));
    }

    return Future.all(resolveContract(), resolveContractParts()).compose(v -> buildOpenAPIContract());
  }

  private Future<OpenAPIContract> buildOpenAPIContract() {
    OpenAPIVersion version = OpenAPIVersion.fromContract(contract);
    String baseUri = "app://";

    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<OpenAPIContract> promise = ctx.promise();

    version.getRepository(vertx, baseUri)
        .compose(repository -> {
          var validationFutures = additionalContractParts.entrySet()
              .stream()
              .map(entry -> version.validateAdditionalContractPart(vertx, repository, entry.getValue())
                  .compose(v -> vertx.executeBlocking(
                      () -> repository.dereference(entry.getKey(), JsonSchema.of(entry.getKey(), entry.getValue())))))
              .collect(Collectors.toList());
          return Future.all(validationFutures).map(repository);
        }).compose(repository -> version.validateContract(vertx, repository, contract).compose(res -> {
          try {
            res.checkValidity();
            return version.resolve(vertx, repository, contract);
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

  private Future<Void> resolveContract() {
    if (contractPath == null) {
      return succeededFuture();
    }
    return Utils.readYamlOrJson(vertx, contractPath).onSuccess(c -> contract = c).mapEmpty();
  }

  private Future<Void> resolveContractParts() {
    if (additionalContractPartPaths.isEmpty()) {
      return succeededFuture();
    }
    return Future.all(additionalContractPartPaths.entrySet().stream()
        .map(e -> Utils.readYamlOrJson(vertx, e.getValue())
            .map(c -> additionalContractParts.put(e.getKey(), c)))
        .collect(Collectors.toList())).mapEmpty();
  }
}
