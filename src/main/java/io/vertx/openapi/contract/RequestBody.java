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
import java.util.Map;

/**
 * This interface represents the most important attributes of an OpenAPI Operation.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#request-body-Object">Operation V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#request-body-Object">Operation V3.0</a>
 */
@VertxGen
public interface RequestBody extends OpenAPIObject {

  /**
   * @return true if the request body is required in the request, otherwise false.
   */
  boolean isRequired();

  /**
   * @return a map containing descriptions of potential request payloads. The key is a media type or media
   * type range and the value describes it.
   */
  Map<String, MediaType> getContent();

  /**
   * This method tries to determine the best fitting {@link MediaType} based on the passed content type.
   * <br>
   * <br>
   * This is necessary, to avoid that an incoming request of type <i>application/json; charset=utf-8</i> is failing,
   * because we only declared <i>application/json</i> in the OpenAPI spec.
   * <br>
   * <br>
   * <b>Important:</b> If <i>application/json</i> is declared in the contract, <i>application/json; charset=utf-8</i>
   * would fit, but not the other way around.
   *
   * @return A fitting media type, or null.
   */
  MediaType determineContentType(String mediaTypeIdentifier);
}
