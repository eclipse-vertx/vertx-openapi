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

  // ==================== resolveCharset 测试 ====================

  @Test
  void testResolveCharsetWithExplicitCharset() {
    assertThat(resolveCharset("application/x-www-form-urlencoded; charset=UTF-8"))
        .isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void testResolveCharsetWithISO8859() {
    assertThat(resolveCharset("application/x-www-form-urlencoded; charset=ISO-8859-1"))
        .isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  void testResolveCharsetInvalidCharset() {
    assertThat(resolveCharset("application/x-www-form-urlencoded; charset=invalid-charset"))
        .isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void testResolveCharsetNoCharset() {
    assertThat(resolveCharset("application/x-www-form-urlencoded"))
        .isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  void testResolveCharsetNullContentType() {
    assertThat(resolveCharset(null)).isEqualTo(StandardCharsets.UTF_8);
  }

  // ==================== checkSyntacticalCorrectness 异常测试 ====================

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

  // ==================== transform 基础测试 ====================

  @Test
  void testTransformBasicFormData() {
    String body = "client_id=26d0bcefd30a4f27897eb9b0e89b53d9&username=admin&password=test123";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("client_id", "26d0bcefd30a4f27897eb9b0e89b53d9")
        .put("username", "admin")
        .put("password", "test123");

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformWithUrlEncoding() {
    String body = "email=user%40example.com&message=hello%20world&special=%26%3D%2F";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("email", "user@example.com")
        .put("message", "hello world")
        .put("special", "&=/");

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  // ==================== transform 数组标记测试 ====================

  @Test
  void testTransformWithArrayNotation() {
    String body = "tags[]=java&tags[]=vertx&tags[]=openapi";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("tags", new JsonArray().add("java").add("vertx").add("openapi"));

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformWithDuplicateKeys() {
    String body = "key=value1&key=value2&key=value3";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("key", new JsonArray().add("value1").add("value2").add("value3"));

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformMixedArrayNotationAndDuplicate() {
    String body = "items[]=a&items=b&items[]=c";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("items", new JsonArray().add("a").add("b").add("c"));

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  // ==================== transform 边界条件测试 ====================

  @Test
  void testTransformEmptyBody() {
    Buffer buffer = Buffer.buffer("");
    String contentType = "application/x-www-form-urlencoded";

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(new JsonObject());
  }

  @Test
  void testTransformNullBody() {
    String contentType = "application/x-www-form-urlencoded";

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, null, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(new JsonObject());
  }

  @Test
  void testTransformKeyWithoutValue() {
    String body = "key1&key2=value2";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("key1", "")
        .put("key2", "value2");

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformValueWithEquals() {
    String body = "equation=1%2B1=2&url=http://example.com?a=1%26b=2";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("equation", "1+1=2")
        .put("url", "http://example.com?a=1&b=2");

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  // ==================== transform JSON 值测试 ====================

  @Test
  void testTransformWithJsonObjectValue() {
    String body = "data=%7B%22nested%22%3A%22value%22%2C%22num%22%3A123%7D";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("data", new JsonObject().put("nested", "value").put("num", 123));

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformWithJsonArrayValue() {
    String body = "items=%5B1%2C2%2C3%5D&name=test";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("items", new JsonArray().add(1).add(2).add(3))
        .put("name", "test");

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  // ==================== 真实登录请求场景测试 ====================

  @Test
  void testTransformLoginExample() {
    String body = "client_id=26d0bcefd30a4f27897eb9b0e89b53d9" +
        "&client_secret=76erkSZyFOgBVMDmVlZ5CJpLkOtOly1ufRKK8JiAvArI7rXo3y76DsfyzXE2FJl1" +
        "&username=admin" +
        "&password=Vertx%402026";
    Buffer buffer = Buffer.buffer(body);
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("client_id", "26d0bcefd30a4f27897eb9b0e89b53d9")
        .put("client_secret", "76erkSZyFOgBVMDmVlZ5CJpLkOtOly1ufRKK8JiAvArI7rXo3y76DsfyzXE2FJl1")
        .put("username", "admin")
        .put("password", "Vertx@2026");

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  // ==================== Content-Type 变体测试 ====================

  @ParameterizedTest
  @ValueSource(strings = {
      "application/x-www-form-urlencoded",
      "application/x-www-form-urlencoded; charset=UTF-8",
  })
  void testTransformWithVariousContentTypes(String contentType) {
    String body = "key=value";
    Buffer buffer = Buffer.buffer(body);

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    JsonObject result = (JsonObject) analyser.transform();
    assertThat(result.getString("key")).isEqualTo("value");
  }

  // ==================== 复杂场景测试 ====================

  @Test
  void testTransformComplexScenario() throws IOException {
    Buffer buffer = Buffer.buffer(Files.readString(TEST_RESOURCE_PATH.resolve("form_urlencoded_complex.txt")));
    String contentType = "application/x-www-form-urlencoded";

    JsonObject expected = new JsonObject()
        .put("grant_type", "password")
        .put("scope", "read:user write:user")
        .put("roles", new JsonArray().add("admin").add("user"))
        .put("settings", new JsonObject().put("theme", "dark").put("notifications", true));

    XWwwFormUrlencodedAnalyser analyser = new XWwwFormUrlencodedAnalyser(contentType, buffer, REQUEST);
    analyser.checkSyntacticalCorrectness();

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }
}
