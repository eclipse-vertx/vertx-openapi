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
import io.vertx.core.http.HttpServerResponse;
import java.util.Map;

@VertxGen
public interface ValidatedResponse {

  /**
   * @return the header parameters.
   */
  Map<String, ResponseParameter> getHeaders();

  /**
   * @return the body.
   */
  ResponseParameter getBody();

  /**
   * Add all parameters from the validated response to the passed {@link HttpServerResponse} and send it.
   *
   * @param serverResponse The related response
   * @return A succeeded Future when the response was sent successfully, otherwise a failed one.
   */
  Future<Void> send(HttpServerResponse serverResponse);
}
