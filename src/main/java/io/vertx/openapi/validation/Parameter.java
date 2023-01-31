/*
 * Copyright (c) Contributors to the Eclipse Foundation
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

import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@VertxGen
interface Parameter {
  /**
   * @return null if value is not a {@link String}, otherwise it returns value
   */
  @Nullable
  default String getString() {
    return isString() ? (String) get() : null;
  }

  /**
   * @return true if value of this instance is a {@link String} instance
   */
  default boolean isString() {
    return !isNull() && get() instanceof String;
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Integer}
   */
  @Nullable
  default Integer getInteger() {
    return isNumber() ? ((Number) get()).intValue() : null;
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Long}
   */
  @Nullable
  default Long getLong() {
    return isNumber() ? ((Number) get()).longValue() : null;
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Float}
   */
  @Nullable
  default Float getFloat() {
    return isNumber() ? ((Number) get()).floatValue() : null;
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Double}
   */
  @Nullable
  default Double getDouble() {
    return isNumber() ? ((Number) get()).doubleValue() : null;
  }

  /**
   * @return true if value of this instance is a {@link Number} instance
   */
  default boolean isNumber() {
    return !isNull() && get() instanceof Number;
  }

  /**
   * @return null if value is not a {@link Boolean}, otherwise it returns value
   */
  @Nullable
  default Boolean getBoolean() {
    return isBoolean() ? (Boolean) get() : null;
  }

  /**
   * @return true if value of this instance is a {@link Boolean} instance
   */
  default boolean isBoolean() {
    return !isNull() && get() instanceof Boolean;
  }

  /**
   * @return null if value is not a {@link JsonObject}, otherwise it returns value
   */
  @Nullable
  default JsonObject getJsonObject() {
    return isJsonObject() ? (JsonObject) get() : null;
  }

  /**
   * @return true if value of this instance is a {@link JsonObject} instance
   */
  default boolean isJsonObject() {
    return !isNull() && get() instanceof JsonObject;
  }

  /**
   * @return null if value is not a {@link JsonArray}, otherwise it returns value
   */
  @Nullable
  default JsonArray getJsonArray() {
    return isJsonArray() ? (JsonArray) get() : null;
  }

  /**
   * @return true if value of this instance is a {@link JsonArray} instance
   */
  default boolean isJsonArray() {
    return !isNull() && get() instanceof JsonArray;
  }

  /**
   * @return null if value is not a {@link Buffer}, otherwise it returns value
   */
  @Nullable
  default Buffer getBuffer() {
    return isBuffer() ? (Buffer) get() : null;
  }

  /**
   * @return true if value of this instance is a {@link Buffer} instance
   */
  default boolean isBuffer() {
    return !isNull() && get() instanceof Buffer;
  }

  /**
   * @return true if value is null
   */
  default boolean isNull() {
    return get() == null;
  }

  /**
   * @return true if it's an empty {@link String}, an empty {@link JsonObject} / {@link JsonArray}, an empty {@link Buffer} or it's null
   */
  default boolean isEmpty() {
    return isNull() ||
      (isString() && getString().isEmpty()) ||
      (isJsonObject() && getJsonObject().isEmpty()) ||
      (isJsonArray() && getJsonArray().isEmpty()) ||
      (isBuffer() && getBuffer().length() == 0);
  }

  /**
   * @return the plain value
   */
  @CacheReturn
  Object get();
}
