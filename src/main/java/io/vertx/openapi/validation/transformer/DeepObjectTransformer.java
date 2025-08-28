/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.openapi.validation.transformer;

import static io.vertx.json.schema.common.dsl.SchemaType.OBJECT;
import static io.vertx.openapi.contract.Style.DEEP_OBJECT;
import static io.vertx.openapi.validation.ValidatorException.createUnsupportedTransformation;

import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.openapi.contract.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * +------------+---------+--------+----------+-----------+-------------------------------------------+
 * | style      | explode | empty  | string   | array     | object                                    |
 * +------------+---------+--------+----------+-----------+-------------------------------------------+
 * | deepObject | true    | n/a    | n/a      | n/a       | dummy[role]=admin&dummy[firstName]=Alex   |
 * +------------+---------+--------+----------+-----------+-------------------------------------------+
 */
public class DeepObjectTransformer extends ParameterTransformer {

  /**
   * Matches name=[key]=value
   */
  private final Pattern PATTERN = Pattern.compile("(\\w+)\\[(\\w+)]\\s*=\\s*([^&]+)");

  @Override
  public Object transformPrimitive(SchemaType type, String rawValue) {
    // as transformObject calls transformPrimitive internally, we delegate to the base method for type OBJECT
    // to avoid breaking transformObject.
    if (type == OBJECT)
      return super.transformPrimitive(type, rawValue);
    throw createUnsupportedTransformation(DEEP_OBJECT, type);
  }

  @Override
  protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
    Matcher matcher = PATTERN.matcher(rawValue);

    List<String> keysAndValues = new ArrayList<>();
    while (matcher.find()) {
      String name = matcher.group(1);
      String key = matcher.group(2);
      String value = matcher.group(3);

      if (parameter.getName().equals(name)) {
        keysAndValues.add(key);
        keysAndValues.add(value);
      }
    }

    return keysAndValues.toArray(new String[keysAndValues.size()]);
  }

  @Override
  public Object transformArray(Parameter parameter, String rawValue) {
    throw createUnsupportedTransformation(parameter.getStyle(), parameter.getSchemaType());
  }

  @Override
  protected String[] getArrayValues(Parameter parameter, String rawValue) {
    // this is never called due to transformArray overridden and throwing an exception.
    return null;
  }

}
