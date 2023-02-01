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

import java.util.Arrays;
import java.util.function.Predicate;

@VertxGen
public enum Style {
  MATRIX("matrix"), LABEL("label"), FORM("form"), SIMPLE("simple"), SPACE_DELIMITED(
    "spaceDelimited"), PIPE_DELIMITED("pipeDelimited"), DEEP_OBJECT("deepObject");

  private final String openAPIValue;

  Style(String openAPIValue) {
    this.openAPIValue = openAPIValue;
  }

  public static Style parse(String style) {
    Predicate<String> eq = Predicate.isEqual(style);
    // Contract validation happened before, so it will find one of these values.
    return style == null ?
      null :
      Arrays.stream(Style.values()).filter(l -> eq.test(l.toString())).findFirst().orElse(null);
  }

  public static Style defaultByLocation(Location in) {
    switch (in) {
      case COOKIE:
      case QUERY:
        return FORM;
      case PATH:
      case HEADER:
        return SIMPLE;
      default:
        return null;
    }
  }

  @Override
  public String toString() {
    return openAPIValue;
  }
}
