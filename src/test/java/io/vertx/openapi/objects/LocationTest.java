package io.vertx.openapi.objects;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.objects.Location.COOKIE;
import static io.vertx.openapi.objects.Location.HEADER;
import static io.vertx.openapi.objects.Location.PATH;
import static io.vertx.openapi.objects.Location.QUERY;

class LocationTest {
  private static Stream<Arguments> provideLocations() {
    return Stream.of(
      Arguments.of(QUERY, "query"),
      Arguments.of(HEADER, "header"),
      Arguments.of(PATH, "path"),
      Arguments.of(COOKIE, "cookie")
    );
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
