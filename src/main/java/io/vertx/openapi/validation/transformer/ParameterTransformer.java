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

package io.vertx.openapi.validation.transformer;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.Parameter;

import static io.vertx.json.schema.common.dsl.SchemaType.STRING;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.validation.ValidatorException.createCantDecodeValue;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

public abstract class ParameterTransformer {

  /**
   * Transforms the raw value from its {@link String} representation into JSON. This method does not only decode a
   * {@link String}, it also takes the different {@link io.vertx.openapi.contract.Style styles} into account}.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transform(Parameter parameter, String rawValue) {
    try {
      switch (parameter.getSchemaType()) {
        case OBJECT:
          return transformObject(parameter, rawValue);
        case ARRAY:
          return transformArray(parameter, rawValue);
        default:
          return transformPrimitive(parameter, rawValue);
      }
    } catch (DecodeException e) {
      throw createCantDecodeValue(parameter);
    }
  }

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be primitive.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transformPrimitive(Parameter parameter, String rawValue) {
    boolean isString = STRING.equals(parameter.getSchemaType());
    if (isString && rawValue.matches("\\w+")) {
      return rawValue;
    }

    try {
      return Json.decodeValue(rawValue);
    } catch (DecodeException de) {
      if (rawValue.isEmpty()) {
        return rawValue;
      } else if ('"' == rawValue.charAt(0)) {
        throw de;
      } else {
        // let's try it as JSON String
        return transformPrimitive(parameter, "\"" + rawValue + "\"");
      }
    }
  }

  protected abstract String[] getArrayValues(Parameter parameter, String rawValue);

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be an array.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transformArray(Parameter parameter, String rawValue) {
    if (rawValue.isEmpty()) {
      return EMPTY_JSON_ARRAY;
    }
    JsonArray array = new JsonArray();
    for (String value : getArrayValues(parameter, rawValue)) {
      array.add(transformPrimitive(parameter, value));
    }
    return array;
  }

  protected abstract String[] getObjectKeysAndValues(Parameter parameter, String rawValue);

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be an object.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transformObject(Parameter parameter, String rawValue) {
    if (rawValue.isEmpty()) {
      return EMPTY_JSON_OBJECT;
    }

    String[] keysAndValues = getObjectKeysAndValues(parameter, rawValue);
    if (keysAndValues.length % 2 != 0) {
      throw createInvalidValueFormat(parameter);
    }
    JsonObject object = new JsonObject();
    for (int i = 0; i < keysAndValues.length; i = i + 2) {
      object.put(keysAndValues[i], transformPrimitive(parameter, keysAndValues[i + 1]));
    }
    return object;
  }
}
