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

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import io.vertx.openapi.validation.impl.ValidatableResponseImpl;
import java.util.Map;
import java.util.Optional;

@VertxGen
public interface ValidatableResponse {

  /**
   * Creates a new {@link ValidatableResponse} object based on the passed parameters.
   *
   * @param statusCode The related status code
   * @return a {@link ValidatableResponse} object
   */
  static ValidatableResponse create(int statusCode) {
    return create(statusCode, null, null, null);
  }

  /**
   * Creates a new {@link ValidatableResponse} object based on the passed parameters.
   *
   * @param statusCode The related status code
   * @param headers    The related headers
   * @return a {@link ValidatableResponse} object
   */
  static ValidatableResponse create(int statusCode, Map<String, String> headers) {
    return create(statusCode, headers, null, null);
  }

  /**
   * Creates a new {@link ValidatableResponse} object based on the passed parameters.
   *
   * @param statusCode  The related status code
   * @param body        The related body
   * @param contentType The related content type
   * @return a {@link ValidatableResponse} object
   * @throws IllegalArgumentException in case body is passed without contentType.
   */
  static ValidatableResponse create(int statusCode, Buffer body, String contentType) {
    return create(statusCode, null, body, contentType);
  }

  /**
   * Creates a new {@link ValidatableResponse} object based on the passed parameters.
   * <p>
   *
   * @param statusCode  The related status code
   * @param headers     The related headers
   * @param body        The related body
   * @param contentType The related content type
   * @return a {@link ValidatableResponse} object
   * @throws IllegalArgumentException in case body is passed without contentType.
   */
  static ValidatableResponse create(int statusCode, Map<String, String> headers, Buffer body, String contentType) {
    Map<String, ResponseParameter> transformedHeaders =
        Optional.ofNullable(headers).orElse(emptyMap()).entrySet().stream().collect(toMap(
            entry -> entry.getKey().toLowerCase(), entry -> new RequestParameterImpl(entry.getValue())));

    if (body != null && contentType == null) {
      throw new IllegalArgumentException("When a body is passed, the content type MUST be specified");
    }

    return new ValidatableResponseImpl(statusCode, transformedHeaders, new RequestParameterImpl(body), contentType);
  }

  /**
   * @return the header parameters.
   */
  Map<String, ResponseParameter> getHeaders();

  /**
   * @return the body.
   */
  ResponseParameter getBody();

  @Nullable
  String getContentType();

  int getStatusCode();
}
