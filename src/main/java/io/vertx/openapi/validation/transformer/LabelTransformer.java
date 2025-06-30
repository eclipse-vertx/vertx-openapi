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

import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

import io.vertx.openapi.contract.Parameter;

/**
 * +--------+---------+--------+-------------+------------------------------+-------------------------------+
 * | style  | explode | empty  | string      | array                        | object                        |
 * +--------+---------+--------+-------------+------------------------------+-------------------------------+
 * | label  | false   | .      | .blue       | .blue,black,brown (RFC-6570) | .R,100,G,200,B,150            |
 * +--------+---------+--------+-------------+------------------------------+-------------------------------+
 * | label  | true    | .      | .blue       | .blue.black.brown            | .R=100.G=200.B=150            |
 * +--------+---------+--------+-------------+------------------------------+-------------------------------+
 */
public class LabelTransformer extends ParameterTransformer {

  private static boolean startsWithDot(String value) {
    return '.' == value.charAt(0);
  }

  @Override
  public Object transform(Parameter parameter, String rawValue) {
    if (!rawValue.isEmpty() && startsWithDot(rawValue)) {
      return super.transform(parameter, rawValue.substring(1));
    } else {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  protected String[] getArrayValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split("\\.") : rawValue.split(",");
  }

  @Override
  protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split("[=|.]") : rawValue.split(",");
  }
}
