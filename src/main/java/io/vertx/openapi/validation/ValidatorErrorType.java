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

@VertxGen
public enum ValidatorErrorType {
  /**
   * A required parameter was not part of the request
   */
  MISSING_REQUIRED_PARAMETER,

  /**
   * The format of the related value does not fit to the expected {@link io.vertx.openapi.contract.Style}
   */
  INVALID_VALUE_FORMAT,

  /**
   * The format of the related value is not yet supported.
   */
  UNSUPPORTED_VALUE_FORMAT,

  /**
   * The value of the related parameter can't be decoded.
   */
  ILLEGAL_VALUE,

  /**
   * The value of the related parameter does not fit to the schema.
   */
  INVALID_VALUE,

  /**
   * The request can't get validated due to missing operation information.
   */
  MISSING_OPERATION
}
