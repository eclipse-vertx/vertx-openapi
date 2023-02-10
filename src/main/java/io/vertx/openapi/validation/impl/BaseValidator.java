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

package io.vertx.openapi.validation.impl;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.validation.transformer.ApplicationJsonTransformer;
import io.vertx.openapi.validation.transformer.BodyTransformer;

import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.openapi.validation.ValidatorException.createOperationIdInvalid;

class BaseValidator {
  protected final Vertx vertx;
  protected final OpenAPIContract contract;
  protected final Map<String, BodyTransformer> bodyTransformers;

  public BaseValidator(Vertx vertx, OpenAPIContract contract) {
    this.vertx = vertx;
    this.contract = contract;

    bodyTransformers = new HashMap<>();
    bodyTransformers.put(APPLICATION_JSON.toString(), new ApplicationJsonTransformer());
  }

  // VisibleForTesting
  Future<Operation> getOperation(String operationId) {
    Operation operation = contract.operation(operationId);
    if (operation == null) {
      return failedFuture(createOperationIdInvalid(operationId));
    }
    return succeededFuture(operation);
  }
}
