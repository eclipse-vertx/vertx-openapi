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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.impl.ResponseValidatorImpl;

/**
 * The {@link ResponseValidator} requires the {@link ValidatableResponse parameters} in a specific format to be able to
 * parse and validate them. This is especially true for <i>exploded</i> parameters. The following table shows how the
 * value of a parameter of a response must be stored in a {@link ValidatableResponse} object. For these examples the key
 * of those values is always <i>color</i>.
 * <p></p>
 * These are the initial values for each type:<br>
 * <ul>
 * <li>primitive (string) -> "blue"</li>
 * <li>array -> ["blue","black","brown"]</li>
 * <li>object -> { "R": 100, "G": 200, "B": 150 }</li>
 * </ul>
 * For header parameters {@link ValidatableRequest#getHeaders()}
 * <pre>
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | style  | explode | empty | primitive | array                              | object                  |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | simple | false   |       | blue      | blue,black,brown                   | R,100,G,200,B,150       |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | simple | true    |       | blue      | blue,black,brown                   | R=100,G=200,B=150       |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * </pre>
 */
public interface ResponseValidator {

  /**
   * Create a new {@link ResponseValidator}.
   *
   * @param vertx    the related Vert.x instance
   * @param contract the related {@link OpenAPIContract}
   * @return an instance of {@link ResponseValidator}.
   */
  static ResponseValidator create(Vertx vertx, OpenAPIContract contract) {
    return new ResponseValidatorImpl(vertx, contract);
  }

  /**
   * Validates the passed response parameters against the operation defined in the related OpenAPI contract.
   *
   * @param params      the response parameters to validate.
   * @param operationId the id of the related operation.
   * @return A succeeded Future with the parsed and validated response parameters, or a failed Future containing ValidationException.
   */
  Future<ValidatedResponse> validate(ValidatableResponse params, String operationId);
}
