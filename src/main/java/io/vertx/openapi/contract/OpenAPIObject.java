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

import io.vertx.core.json.JsonObject;

public interface OpenAPIObject {

  /**
   * Returns the part of the related OpenAPI specification which is represented by the OpenAPI object that is
   * implementing this interface.
   *
   * @return a {@link  JsonObject} that represents this part of the related OpenAPI specification.
   */
  JsonObject getOpenAPIModel();
}
