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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.function.Supplier;

interface Parameter {
  /**
   * @return null if value is not a {@link String}, otherwise it returns value
   */
  @Nullable
  default String getString() {
    return getString(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link String} or is
   * null, otherwise it returns the value.
   */
  default String getString(Supplier<String> defaultValue) {
    return isString() ? (String) get() : defaultValue.get();
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
    return getInteger(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link Number} or is
   * null, otherwise it returns the value.
   */
  default Integer getInteger(Supplier<Integer> defaultValue) {
    return isNumber() ? Integer.valueOf(((Number) get()).intValue()) : defaultValue.get();
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Long}
   */
  @Nullable
  default Long getLong() {
    return getLong(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link Number} or is
   * null, otherwise it returns the value.
   */
  default Long getLong(Supplier<Long> defaultValue) {
    return isNumber() ? Long.valueOf(((Number) get()).longValue()) : defaultValue.get();
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Float}
   */
  @Nullable
  default Float getFloat() {
    return getFloatOrDefault(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link Number} or is
   * null, otherwise it returns the value.
   */
  default Float getFloatOrDefault(Supplier<Float> defaultValue) {
    return isNumber() ? Float.valueOf(((Number) get()).floatValue()) : defaultValue.get();
  }

  /**
   * @return null if value is not a {@link Number}, otherwise it returns value as {@link Double}
   */
  @Nullable
  default Double getDouble() {
    return getDouble(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link Number} or is
   * null, otherwise it returns the value.
   */
  default Double getDouble(Supplier<Double> defaultValue) {
    return isNumber() ? Double.valueOf(((Number) get()).doubleValue()) : defaultValue.get();
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
    return getBoolean(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link Boolean} or is
   * null, otherwise it returns the value.
   */
  default Boolean getBoolean(Supplier<Boolean> defaultValue) {
    return isBoolean() ? (Boolean) get() : defaultValue.get();
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
    return getJsonObject(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link JsonObject} or is
   * null, otherwise it returns the value.
   */
  default JsonObject getJsonObject(Supplier<JsonObject> defaultValue) {
    return isJsonObject() ? (JsonObject) get() : defaultValue.get();
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
    return getJsonArray(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link JsonArray} or is
   * null, otherwise it returns the value.
   */
  default JsonArray getJsonArray(Supplier<JsonArray> defaultValue) {
    return isJsonArray() ? (JsonArray) get() : defaultValue.get();
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
    return getBuffer(() -> null);
  }

  /**
   * @return the default supplied by a {@link java.util.function.Supplier} if value is not a {@link Buffer} or is
   * null, otherwise it returns the value.
   */
  default Buffer getBuffer(Supplier<Buffer> defaultValue) {
    return isBuffer() ? (Buffer) get() : defaultValue.get();
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
   * @return true if it's an empty {@link String}, an empty {@link JsonObject} / {@link JsonArray}, an empty
   * {@link Buffer} or it's null
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
