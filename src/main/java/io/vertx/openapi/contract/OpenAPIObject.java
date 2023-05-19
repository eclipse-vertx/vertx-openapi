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
import io.vertx.json.schema.impl.JsonObjectProxy;

public interface OpenAPIObject {

  /**
   * Returns the part of the related OpenAPI specification which is represented by the OpenAPI object that is
   * implementing this interface.
   * <p></p>
   * <b>Warning:</b>
   * <ul>
   *   <li> In case the contract <b>contains circular references</b>, the returned object is may of type
   *   {@link JsonObjectProxy}, which has some <b>limitations</b> when it comes to copying or serializing.</li>
   *   <li>Due to these limitations, the reference of the <b>original object is returned</b>. Because of this be very
   *   <b>careful when modifying</b> the returned object.</li>
   * </ul>
   *
   * @return a {@link JsonObject} that represents this part of the related OpenAPI specification.
   */
  JsonObject getOpenAPIModel();
}
