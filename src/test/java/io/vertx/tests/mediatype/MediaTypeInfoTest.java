/*
 * Copyright (c) 2025, Lukas Jelonek
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.tests.mediatype;

import static com.google.common.truth.Truth.assertThat;

import io.vertx.openapi.mediatype.MediaTypeInfo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MediaTypeInfoTest {

  static Stream<Arguments> mediatypes() {
    return Stream.of(
        Arguments.of("text/html", "text", "html", null, Map.of()),
        Arguments.of("application/json", "application", "json", null, Map.of()),
        Arguments.of("image/png", "image", "png", null, Map.of()),
        Arguments.of("application/vnd.api+json", "application", "vnd.api", "json", Map.of()),
        Arguments.of("text/plain; charset=UTF-8", "text", "plain", null, Map.of("charset", "UTF-8")),
        Arguments.of("audio/*", "audio", "*", null, Map.of()),
        Arguments.of("application/xhtml+xml", "application", "xhtml", "xml", Map.of()),
        Arguments.of("application/xml; charset=ISO-8859-1", "application", "xml", null,
            Map.of("charset", "ISO-8859-1")),
        Arguments.of("video/*; codec=H.264", "video", "*", null, Map.of("codec", "H.264")),
        Arguments.of("application/octet-stream", "application", "octet-stream", null, Map.of()),
        Arguments.of("application/json; charset=UTF-8; version=1.0", "application", "json", null,
            orderedMap("charset", "UTF-8", "version", "1.0")));
  }

  private static Map<String, String> orderedMap(String k1, String v1, String k2, String v2) {
    var m = new LinkedHashMap<String, String>();
    m.put(k1, v1);
    m.put(k2, v2);
    return m;
  }

  @ParameterizedTest
  @MethodSource("mediatypes")
  void should_parse_mediatype_correctly(String mediatypestring, String type, String subtype, String suffix,
      Map<String, String> parameters) {
    var info = MediaTypeInfo.of(mediatypestring);
    assertThat(info.type()).isEqualTo(type);
    assertThat(info.subtype()).isEqualTo(subtype);
    assertThat(info.suffix()).isEqualTo(Optional.ofNullable(suffix));
    assertThat(info.parameters()).isEqualTo(parameters);
  }

  @ParameterizedTest
  @MethodSource("mediatypes")
  void should_serialize_mediatype_info_correctly(String mediatypestring, String type, String subtype, String suffix,
      Map<String, String> parameters) {
    var info = new MediaTypeInfo(type, subtype, suffix, parameters);
    assertThat(info.toString()).isEqualTo(mediatypestring);
  }

  @ParameterizedTest
  @MethodSource("mediatypes")
  void fullType_should_include_type_subtype_suffix(String mediatypestring) {
    var info = MediaTypeInfo.of(mediatypestring);
    assertThat(info.fullType()).isEqualTo(mediatypestring.split(";")[0].trim());
  }

  static Stream<Arguments> compatibility_data() {
    return Stream.of(
        Arguments.of("application/json", "application/json", true),
        Arguments.of("application/json", "application/json; charset=utf-8", true),
        Arguments.of("application/json; charset=iso-8851-1", "application/json; charset=utf-8", false),
        Arguments.of("application/json; charset=utf-8", "application/json", false),
        Arguments.of("application/*", "application/json", true),
        Arguments.of("application/json", "application/*", false),
        Arguments.of("application/*", "text/plain", false),
        Arguments.of("application/*", "text/*", false),
        Arguments.of("application/vnd.example", "application/vnd.example+xml", true),
        Arguments.of("application/vnd.example", "application/vnd.example+json", true),
        Arguments.of("application/vnd.example+json", "application/vnd.example", false),
        Arguments.of("application/vnd.example+json", "application/vnd.example+xml", false),
        Arguments.of("application/vnd.example+json", "application/vnd.example+json", true));
  }

  @ParameterizedTest
  @MethodSource("compatibility_data")
  void doesInclude_should_work_correctly(String src, String other, boolean expected) {
    assertThat(MediaTypeInfo.of(src).doesInclude(MediaTypeInfo.of(other))).isEqualTo(expected);
  }
}
