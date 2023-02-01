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

/**
 * <p>
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 * | style  | explode | empty  | string     | array                              | object                  |
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 * | form   | false   | color= | color=blue | color=blue,black,brown             | color=R,100,G,200,B,150 |
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 * | form   | true    | color= | color=blue | color=blue&color=black&color=brown | R=100&G=200&B=150       |
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 */
public class FormTransformer extends ParameterTransformer {

  private static String buildPrefix(Parameter parameter) {
    return parameter.getName() + "=";
  }

  @Override
  protected String[] getArrayValues(Parameter parameter, String rawValue) {
    if (parameter.isExplode()) {
      String prefix = buildPrefix(parameter);
      return rawValue.substring(prefix.length()).split("&" + prefix);
    } else {
      return rawValue.split(",");
    }
  }

  @Override protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split("[=&]") : rawValue.split(",");
  }
}
