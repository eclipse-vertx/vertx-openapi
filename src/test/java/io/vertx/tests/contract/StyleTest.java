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
import static io.vertx.openapi.contract.Style.DEEP_OBJECT;
import static io.vertx.openapi.contract.Style.FORM;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.contract.Style.PIPE_DELIMITED;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static io.vertx.openapi.contract.Style.SPACE_DELIMITED;

import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.Style;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StyleTest {
  private static Stream<Arguments> provideStyles() {
    return Stream.of(
        Arguments.of(MATRIX, "matrix"),
        Arguments.of(LABEL, "label"),
        Arguments.of(FORM, "form"),
        Arguments.of(SIMPLE, "simple"),
        Arguments.of(SPACE_DELIMITED, "spaceDelimited"),
        Arguments.of(PIPE_DELIMITED, "pipeDelimited"),
        Arguments.of(DEEP_OBJECT, "deepObject"));
  }

  private static Stream<Arguments> defaultStylesByLocation() {
    return Stream.of(
        Arguments.of(QUERY, FORM),
        Arguments.of(HEADER, SIMPLE),
        Arguments.of(PATH, SIMPLE),
        Arguments.of(COOKIE, FORM));
  }

  @ParameterizedTest(name = "{index} defaultByLocation should return the correct Style for Location Style {0}")
  @MethodSource("defaultStylesByLocation")
  void testDefaultByLocationStyle(Location location, Style expectedStyle) {
    assertThat(Style.defaultByLocation(location)).isEqualTo(expectedStyle);
  }

  @ParameterizedTest(name = "{index} toString should transform correct {0}")
  @MethodSource("provideStyles")
  void testToStringStyle(Style style, String expected) {
    assertThat(style.toString()).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{index} should parse value correct {1}")
  @MethodSource("provideStyles")
  void testParseStyle(Style expected, String value) {
    assertThat(Style.parse(value)).isEqualTo(expected);
  }
}
