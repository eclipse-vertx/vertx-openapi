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

package io.vertx.tests.contract;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.contract.Location.COOKIE;
import static io.vertx.openapi.contract.Location.HEADER;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Location.QUERY;

import io.vertx.openapi.contract.Location;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LocationTest {
  private static Stream<Arguments> provideLocations() {
    return Stream.of(
        Arguments.of(QUERY, "query"),
        Arguments.of(HEADER, "header"),
        Arguments.of(PATH, "path"),
        Arguments.of(COOKIE, "cookie"));
  }

  @ParameterizedTest(name = "{index} toString should transform correct {0}")
  @MethodSource("provideLocations")
  void testToStringLocation(Location location, String expected) {
    assertThat(location.toString()).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{index} should parse value correct {1}")
  @MethodSource("provideLocations")
  void testParseLocation(Location expected, String value) {
    assertThat(Location.parse(value)).isEqualTo(expected);
  }
}
