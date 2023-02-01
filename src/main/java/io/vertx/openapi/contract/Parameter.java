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
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaType;

/**
 * This interface represents the most important attributes of an OpenAPI Parameter.
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#parameter-Object">Parameter V3.1</a>
 * <br>
 * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#parameter-Object">Parameter V3.0</a>
 */
@VertxGen
public interface Parameter extends OpenAPIObject {

  /**
   * @return name of this parameter
   */
  String getName();

  /**
   * @return location of this parameter
   */
  Location getIn();

  /**
   * @return true if the parameter is required, otherwise false;
   */
  boolean isRequired();

  /**
   * @return style of this parameter
   */
  Style getStyle();

  /**
   * @return true if the parameter should become exploded, otherwise false;
   */
  boolean isExplode();

  /**
   * @return the {@link JsonSchema} of the parameter
   */
  JsonSchema getSchema();

  /**
   * @return the {@link SchemaType} of the parameter
   */
  SchemaType getSchemaType();
}
