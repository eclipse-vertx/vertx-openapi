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
import java.util.List;
import java.util.Map;

@VertxGen
public interface Response extends OpenAPIObject {

  /**
   * @return the headers of the response.
   */
  List<Parameter> getHeaders();

  /**
   * @return a map containing descriptions of potential response payloads. The key is a media type or media
   * type range and the value describes it.
   */
  Map<String, MediaType> getContent();
}
