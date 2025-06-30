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
public enum Location {
  QUERY, HEADER, PATH, COOKIE;

  public static Location parse(String location) {
    Predicate<String> eq = Predicate.isEqual(location);
    // Contract validation happened before, so it will find one of these values.
    return location == null ? null
        : Arrays.stream(Location.values()).filter(l -> eq.test(l.toString())).findFirst().orElse(null);
  }

  @Override
  public String toString() {
    return super.name().toLowerCase();
  }
}
