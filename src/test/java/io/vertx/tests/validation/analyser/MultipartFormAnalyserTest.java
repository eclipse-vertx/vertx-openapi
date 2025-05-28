/*
 * Copyright (c) 2024, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.tests.validation.analyser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatorErrorType;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.mediatype.impl.MultipartFormAnalyser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.validation.ValidationContext.REQUEST;
import static io.vertx.openapi.mediatype.impl.MultipartFormAnalyser.extractBoundary;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipartFormAnalyserTest {
  private static final Path TEST_RESOURCE_PATH = getRelatedTestResourcePath(MultipartFormAnalyserTest.class);

  @Test
  void testExtractBoundary() {
    String boundary = "abcde12345";
    assertThat(extractBoundary("multipart/form-data; boundary=" + boundary)).isEqualTo(boundary);

    assertThat(extractBoundary("multipart/form-data; boundary= ")).isNull();
    assertThat(extractBoundary("multipart/form-data; boundary:" + boundary)).isNull();
    assertThat(extractBoundary("multipart/form-data")).isNull();
  }

  static Stream<Arguments> testCheckSyntacticalCorrectnessThrowIfContentTypeisMissing() {
    return Stream.of(Arguments.of((String) null), Arguments.of(""), Arguments.of("application/json"));
  }

  @ParameterizedTest
  @MethodSource
  void testCheckSyntacticalCorrectnessThrowIfContentTypeisMissing(String contentType) {
    ValidatorException exception =
      assertThrows(ValidatorException.class,
        () -> new MultipartFormAnalyser(contentType, null, REQUEST).checkSyntacticalCorrectness());

    String expectedMsg = "The expected multipart/form-data request doesn't contain the required content-type header.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  static Stream<Arguments> testCheckSyntacticalCorrectnessThrowIfBoundaryIsMissing() {
    return Stream.of(Arguments.of("multipart/form-data;"), Arguments.of("multipart/form-data; boundary= "));
  }

  @ParameterizedTest
  @MethodSource
  void testCheckSyntacticalCorrectnessThrowIfBoundaryIsMissing(String contentType) {
    ValidatorException exception =
      assertThrows(ValidatorException.class,
        () -> new MultipartFormAnalyser(contentType, null, REQUEST).checkSyntacticalCorrectness());

    String expectedMsg = "The expected multipart/form-data request doesn't contain the required boundary information.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest
  @ValueSource(strings = {"multipart.txt", "multipart_extended_content_type.txt"})
  void testTransform(String file) throws IOException {
    Buffer multipartBody = Buffer.buffer(Files.readString(TEST_RESOURCE_PATH.resolve(file)));
    String contentType = "multipart/form-data; boundary=abcde12345";
    JsonObject expected = new JsonObject()
      .put("id", "123e4567-e89b-12d3-a456-426655440000")
      .put("address", new JsonObject()
        .put("street", "3, Garden St")
        .put("city", "Hillsbery, UT"));

    MultipartFormAnalyser analyser = new MultipartFormAnalyser(contentType, multipartBody, REQUEST);
    analyser.checkSyntacticalCorrectness(); // must always be executed before transform

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformOctetStream() throws IOException {
    Buffer multipartBody = Buffer.buffer(Files.readString(TEST_RESOURCE_PATH.resolve("multipart_octet_stream.txt")));
    String contentType = "multipart/form-data; boundary=abcde12345";
    JsonObject expected = new JsonObject()
      .put("street", "3, Garden St")
      .put("city", "Hillsbery, UT");

    MultipartFormAnalyser analyser = new MultipartFormAnalyser(contentType, multipartBody, REQUEST);
    analyser.checkSyntacticalCorrectness(); // must always be executed before transform

    JsonObject result = (JsonObject) analyser.transform();
    assertThat(result.getBuffer("address").toJsonObject()).isEqualTo(expected);
  }

  @Test
  void testTransformContinueWhenBodyEmpty() throws IOException {
    Buffer multipartBody = Buffer.buffer(Files.readString(TEST_RESOURCE_PATH.resolve("multipart_id_no_body.txt")));
    String contentType = "multipart/form-data; boundary=abcde12345";
    JsonObject expected = new JsonObject()
      .put("address", new JsonObject()
        .put("street", "3, Garden St")
        .put("city", "Hillsbery, UT"));

    MultipartFormAnalyser analyser = new MultipartFormAnalyser(contentType, multipartBody, REQUEST);
    analyser.checkSyntacticalCorrectness(); // must always be executed before transform

    assertThat((JsonObject) analyser.transform()).isEqualTo(expected);
  }

  @Test
  void testTransformPartWithInvalidContentType() throws IOException {
    Buffer multipartBody = Buffer.buffer(Files.readString(TEST_RESOURCE_PATH.resolve(
      "multipart_part_invalid_contenttype.txt")));
    String contentType = "multipart/form-data; boundary=abcde12345";

    MultipartFormAnalyser analyser = new MultipartFormAnalyser(contentType, multipartBody, REQUEST);
    analyser.checkSyntacticalCorrectness(); // must always be executed before transform
    ValidatorException exception = assertThrows(ValidatorException.class, analyser::transform);

    String expectedMsg = "The content type text/html of property id is not yet supported.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
