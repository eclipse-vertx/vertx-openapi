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

package io.vertx.openapi.validation.transformer;

import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;

public interface BodyTransformer {

  /**
   * Transforms the body of a request into a format that can be validated by the
   * {@link io.vertx.openapi.validation.RequestValidator}.
   *
   * @param type    the media type of the body.
   * @param request the request with the body to transform.
   * @return the transformed body.
   */
  Object transformRequest(MediaType type, ValidatableRequest request);

  /**
   * Transforms the body of a response into a format that can be validated by the
   * {@link io.vertx.openapi.validation.ResponseValidator}.
   *
   * @param type     the media type of the body.
   * @param response the response with the body to transform.
   * @return the transformed body.
   */
  Object transformResponse(MediaType type, ValidatableResponse response);
}
