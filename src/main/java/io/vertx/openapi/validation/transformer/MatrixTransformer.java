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

import io.vertx.openapi.contract.Parameter;

import static io.vertx.json.schema.common.dsl.SchemaType.OBJECT;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

/**
 * <p>
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | style  | explode | empty  | string      | array                               | object                   |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | matrix | false   | ;color | ;color=blue | ;color=blue,black,brown             | ;color=R,100,G,200,B,150 |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | matrix | true    | ;color | ;color=blue | ;color=blue;color=black;color=brown | ;R=100;G=200;B=150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 */
public class MatrixTransformer extends ParameterTransformer {

  public String buildPrefix(Parameter parameter) {
    if (parameter.isExplode() && parameter.getSchemaType() == OBJECT) {
      return ";";
    }
    return ";" + parameter.getName() + "=";
  }

  @Override
  public Object transform(Parameter parameter, String rawValue) {
    String prefix = buildPrefix(parameter);
    if (!rawValue.isEmpty() && rawValue.startsWith(prefix)) {
      return super.transform(parameter, rawValue.substring(prefix.length()));
    } else {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  protected String[] getArrayValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split(buildPrefix(parameter)) : rawValue.split(",");

  }

  @Override protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split("(" + buildPrefix(parameter) + "|=)") : rawValue.split(",");
  }
}
