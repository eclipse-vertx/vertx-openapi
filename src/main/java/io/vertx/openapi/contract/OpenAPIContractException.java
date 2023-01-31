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

package io.vertx.openapi.contract;

import static io.vertx.openapi.contract.ContractErrorType.INVALID_SPEC;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_FEATURE;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_SPEC;

public class OpenAPIContractException extends RuntimeException {

  private final ContractErrorType type;

  public OpenAPIContractException(String message, ContractErrorType type, Throwable cause) {
    super(message, cause);
    this.type = type;
  }

  public static OpenAPIContractException createInvalidContract(String reason) {
    return createInvalidContract(reason, null);
  }

  public static OpenAPIContractException createInvalidContract(String reason, Throwable cause) {
    String msg = "The passed OpenAPI contract is invalid" + (reason == null ? "." : ": " + reason);
    return new OpenAPIContractException(msg, INVALID_SPEC, cause);
  }

  public static OpenAPIContractException createUnsupportedVersion(String version) {
    return new OpenAPIContractException("The version of the passed OpenAPI contract is not supported: " + version,
      UNSUPPORTED_SPEC, null);
  }

  public static OpenAPIContractException createUnsupportedFeature(String feature) {
    return new OpenAPIContractException(
      "The passed OpenAPI contract contains a feature that is not supported: " + feature, UNSUPPORTED_FEATURE, null);
  }

  public static OpenAPIContractException createInvalidStyle(Location in, String allowedStyles) {
    String reason = String.format("The style of a %s parameter MUST be %s", in, allowedStyles);
    return createInvalidContract(reason);
  }

  public ContractErrorType type() {
    return type;
  }
}
