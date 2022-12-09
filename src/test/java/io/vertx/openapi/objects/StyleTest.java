package io.vertx.openapi.objects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.vertx.openapi.objects.Location.COOKIE;
import static io.vertx.openapi.objects.Location.HEADER;
import static io.vertx.openapi.objects.Location.PATH;
import static io.vertx.openapi.objects.Location.QUERY;
import static io.vertx.openapi.objects.Style.DEEP_OBJECT;
import static io.vertx.openapi.objects.Style.FORM;
import static io.vertx.openapi.objects.Style.LABEL;
import static io.vertx.openapi.objects.Style.MATRIX;
import static io.vertx.openapi.objects.Style.PIPE_DELIMITED;
import static io.vertx.openapi.objects.Style.SIMPLE;
import static io.vertx.openapi.objects.Style.SPACE_DELIMITED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StyleTest {
  private static Stream<Arguments> provideStyles() {
    return Stream.of(
      Arguments.of(MATRIX, "matrix"),
      Arguments.of(LABEL, "label"),
      Arguments.of(FORM, "form"),
      Arguments.of(SIMPLE, "simple"),
      Arguments.of(SPACE_DELIMITED, "spaceDelimited"),
      Arguments.of(PIPE_DELIMITED, "pipeDelimited"),
      Arguments.of(DEEP_OBJECT, "deepObject")
    );
  }

  private static Stream<Arguments> defaultStylesByLocation() {
    return Stream.of(
      Arguments.of(QUERY, FORM),
      Arguments.of(HEADER, SIMPLE),
      Arguments.of(PATH, SIMPLE),
      Arguments.of(COOKIE, FORM)
    );
  }

  @ParameterizedTest(name = "{index} defaultByLocation should return the correct Style for Location Style {0}")
  @MethodSource("defaultStylesByLocation")
  void testDefaultByLocationStyle(Location location, Style expectedStyle) {
    assertEquals(expectedStyle, Style.defaultByLocation(location));
  }

  @ParameterizedTest(name = "{index} toString should transform correct {0}")
  @MethodSource("provideStyles")
  void testToStringStyle(Style style, String expected) {
    assertEquals(expected, style.toString());
  }

  @ParameterizedTest(name = "{index} should parse value correct {1}")
  @MethodSource("provideStyles")
  void testParseStyle(Style expected, String value) {
    assertEquals(expected, Style.parse(value));
  }
}
