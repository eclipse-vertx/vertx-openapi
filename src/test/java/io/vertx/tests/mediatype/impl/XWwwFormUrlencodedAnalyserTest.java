/*
 * Copyright (c) 2025, Shi HaiBin
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.tests.mediatype.impl;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.mediatype.impl.XWwwFormUrlencodedAnalyser.resolveCharset;
import static io.vertx.openapi.validation.ValidationContext.REQUEST;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.mediatype.impl.XWwwFormUrlencodedAnalyser;
import io.vertx.openapi.validation.ValidatorErrorType;
import io.vertx.openapi.validation.ValidatorException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class XWwwFormUrlencodedAnalyserTest {
  private static final Path TEST_RESOURCE_PATH = getRelatedTestResourcePath(XWwwFormUrlencodedAnalyserTest.class);
  private static final String APPLICATION_X_WWW_FORM_URL_ENCODED = "application/x-www-form-urlencoded";

  static Stream<Arguments> testResolveCharset() {
    return Stream.of(
        Arguments.of(APPLICATION_X_WWW_FORM_URL_ENCODED + "; charset=UTF-8", StandardCharsets.UTF_8),
        Arguments.of(APPLICATION_X_WWW_FORM_URL_ENCODED + "; charset=ISO-8859-1", StandardCharsets.ISO_8859_1),
        Arguments.of(APPLICATION_X_WWW_FORM_URL_ENCODED + "; charset=invalid-charset", StandardCharsets.UTF_8),
        Arguments.of(APPLICATION_X_WWW_FORM_URL_ENCODED, StandardCharsets.UTF_8),
        Arguments.of(null, StandardCharsets.UTF_8));
  }

  @ParameterizedTest
  @MethodSource
  void testResolveCharset(String contentType, Charset expected) {
    assertThat(resolveCharset(contentType)).isEqualTo(expected);
  }

  // ==================== checkSyntacticalCorrectness - exception tests ====================

  static Stream<Arguments> testCheckSyntacticalCorrectnessThrowIfContentTypeIsMissing() {
    return Stream.of(
        Arguments.of((String) null),
        Arguments.of(""),
        Arguments.of("application/json"),
        Arguments.of("text/plain"));
  }

  @ParameterizedTest
  @MethodSource
  void testCheckSyntacticalCorrectnessThrowIfContentTypeIsMissing(String contentType) {
    ValidatorException exception = assertThrows(ValidatorException.class,
        () -> new XWwwFormUrlencodedAnalyser(contentType, null, REQUEST).checkSyntacticalCorrectness());

    String expectedMsg =
        "The expected application/x-www-form-urlencoded request doesn't contain the required content-type header.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  static Stream<Arguments> testTransform() {
    return Stream.of(
        Arguments.of("basic form data",
            "client_id=26d0bcefd30a4f27897eb9b0e89b53d9&username=admin&password=test123",
            new JsonObject()
                .put("client_id", "26d0bcefd30a4f27897eb9b0e89b53d9")
                .put("username", "admin")
                .put("password", "test123")),
        Arguments.of("URL encoding",
            "email=user%40example.com&message=hello%20world&special=%26%3D%2F",
            new JsonObject()
                .put("email", "user@example.com")
                .put("message", "hello world")
                .put("special", "&=/")),
        Arguments.of("array notation",
            "tags[]=java&tags[]=vertx&tags[]=openapi",
            new JsonObject()
                .put("tags", new JsonArray().add("java").add("vertx").add("openapi"))),
        Arguments.of("duplicate keys",
            "key=value1&key=value2&key=value3",
            new JsonObject()
                .put("key", new JsonArray().add("value1").add("value2").add("value3"))),
        Arguments.of("mixed array notation and duplicate keys",
            "items[]=a&items=b&items[]=c",
            new JsonObject()
                .put("items", new JsonArray().add("a").add("b").add("c"))),
        Arguments.of("empty body",
            "",
            new JsonObject()),
        Arguments.of("null body",
            null,
            new JsonObject()),
        Arguments.of("key without value",
            "key1&key2=value2",
            new JsonObject()
                .put("key1", "")
                .put("key2", "value2")),
        Arguments.of("value with equals sign",
            "equation=1%2B1=2&url=http://example.com?a=1%26b=2",
            new JsonObject()
                .put("equation", "1+1=2")
                .put("url", "http://example.com?a=1&b=2")),
        Arguments.of("JSON object value",
            "data=%7B%22nested%22%3A%22value%22%2C%22num%22%3A123%7D",
            new JsonObject()
                .put("data", new JsonObject().put("nested", "value").put("num", 123))),
        Arguments.of("JSON array value",
            "items=%5B1%2C2%2C3%5D&name=test",
            new JsonObject()
                .put("items", new JsonArray().add(1).add(2).add(3))
                .put("name", "test")),
        Arguments.of("real-world login request",
            "client_id=26d0bcefd30a4f27897eb9b0e89b53d9"
                + "&client_secret=76erkSZyFOgBVMDmVlZ5CJpLkOtOly1ufRKK8JiAvArI7rXo3y76DsfyzXE2FJl1"
                + "&username=admin"
                + "&password=Vertx%402026",
            new JsonObject()
                .put("client_id", "26d0bcefd30a4f27897eb9b0e89b53d9")
                .put("client_secret", "76erkSZyFOgBVMDmVlZ5CJpLkOtOly1ufRKK8JiAvArI7rXo3y76DsfyzXE2FJl1")
                .put("username", "admin")
                .put("password", "Vertx@2026")));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  void testTransform(String description, String body, JsonObject expected) {
    Buffer buffer = body != null ? Buffer.buffer(body) : null;

    XWwwFormUrlencodedAnalyser analyser =
        new XWwwFormUrlencodedAnalyser(APPLICATION_X_WWW_FORM_URL_ENCODED, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  // ==================== transform - Content-Type variant tests ====================

  @ParameterizedTest
  @ValueSource(strings = {
      APPLICATION_X_WWW_FORM_URL_ENCODED,
      APPLICATION_X_WWW_FORM_URL_ENCODED + "; charset=UTF-8",
  })
  void testTransformWithVariousContentTypes(String contentType) {
    String body = "key=value";
    Buffer buffer = Buffer.buffer(body);

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    JsonObject result = (JsonObject) analyser.transform();
    assertThat(result.getString("key")).isEqualTo("value");
  }

  // ==================== transform - complex scenario tests ====================

  @Test
  void testTransformComplexScenario() throws IOException {
    Buffer buffer = Buffer.buffer(Files.readString(TEST_RESOURCE_PATH.resolve("form_urlencoded_complex.txt")));

    JsonObject expected = new JsonObject()
        .put("grant_type", "password")
        .put("scope", "read:user write:user")
        .put("roles", new JsonArray().add("admin").add("user"))
        .put("settings", new JsonObject().put("theme", "dark").put("notifications", true));

    XWwwFormUrlencodedAnalyser analyser =
        new XWwwFormUrlencodedAnalyser(APPLICATION_X_WWW_FORM_URL_ENCODED, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }
}
