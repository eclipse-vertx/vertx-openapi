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

package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.openapi.contract.Operation;

@VertxGen
public interface ValidatableRequest extends ValidatedRequest {

  /**
   * Creates a new {@link ValidatableRequest} object based on the passed {@link HttpServerRequest request} and {@link Operation operation}.
   *
   * @param request   The related request
   * @param operation The related operation
   * @return a {@link ValidatableRequest} object
   */
  static Future<ValidatableRequest> of(HttpServerRequest request, Operation operation) {
    return RequestUtils.extract(request, operation);
  }

  String getContentType();
}
