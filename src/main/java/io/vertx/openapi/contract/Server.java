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

import io.vertx.codegen.annotations.VertxGen;

/**
 * This interface represents the most important attributes of an OpenAPI Server.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#server-Object">Server V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#server-Object">Server V3.0</a>
 */
@VertxGen
public interface Server extends OpenAPIObject {

  /**
   * @return the URL of the related server
   */
  String getURL();

  /**
   * The base path is used to indicate that the location where the OpenAPI contract is served is different
   * from the path specified in the OpenAPI contract.
   *
   * @return the related base path.
   */
  String getBasePath();
}
