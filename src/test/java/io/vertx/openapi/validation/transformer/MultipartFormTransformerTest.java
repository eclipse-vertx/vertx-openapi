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

package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatorErrorType;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.MockHelper.mockValidatableRequest;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.openapi.validation.transformer.MultipartFormTransformer.extractBoundary;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipartFormTransformerTest {
  private static final Path TEST_RESOURCE_PATH = getRelatedTestResourcePath(MultipartFormTransformerTest.class);

  @Test
  void testExtractBoundary() {
    String boundary = "abcde12345";
    assertThat(extractBoundary("multipart/form-data; boundary=" + boundary)).isEqualTo(boundary);

    assertThat(extractBoundary("multipart/form-data; boundary= ")).isNull();
    assertThat(extractBoundary("multipart/form-data; boundary:" + boundary)).isNull();
    assertThat(extractBoundary("multipart/form-data")).isNull();
  }

  static Stream<Arguments> testTransformThrowIfContentTypeisMissing() {
    return Stream.of(Arguments.of((String) null), Arguments.of(""), Arguments.of("application/json"));
  }

  @ParameterizedTest
  @MethodSource
  void testTransformThrowIfContentTypeisMissing(String contentType) {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> MultipartFormTransformer.transform(null, null, contentType,
        "request"));

    String expectedMsg = "The expected multipart/form-data request doesn't contain the required content-type header.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  static Stream<Arguments> testTransformThrowIfBoundaryIsMissing() {
    return Stream.of(Arguments.of("multipart/form-data;"), Arguments.of("multipart/form-data; boundary= "));
  }

  @ParameterizedTest
  @MethodSource
  void testTransformThrowIfBoundaryIsMissing(String contentType) {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> MultipartFormTransformer.transform(null, null, contentType,
        "request"));

    String expectedMsg = "The expected multipart/form-data request doesn't contain the required boundary information.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testTransformRequest() throws IOException {
    Buffer multipartBody = Buffer.buffer(new String(Files.readAllBytes(TEST_RESOURCE_PATH.resolve("multipart.txt"))));
    ValidatableRequest req = mockValidatableRequest(multipartBody, "multipart/form-data; boundary=abcde12345");

    JsonObject expected = new JsonObject()
      .put("id", "123e4567-e89b-12d3-a456-426655440000")
      .put("address", new JsonObject()
        .put("street", "3, Garden St")
        .put("city", "Hillsbery, UT"));

    JsonObject json = (JsonObject) new MultipartFormTransformer().transformRequest(null, req);
    assertThat(json).isEqualTo(expected);
  }

  @Test
  void testTransformRequestContinueWhenBodyEmpty() throws IOException {
    Buffer multipartBody = Buffer.buffer(new String(Files.readAllBytes(TEST_RESOURCE_PATH.resolve("multipart_id_no_body.txt"))));
    ValidatableRequest req = mockValidatableRequest(multipartBody, "multipart/form-data; boundary=abcde12345");

    JsonObject expected = new JsonObject()
      .put("address", new JsonObject()
        .put("street", "3, Garden St")
        .put("city", "Hillsbery, UT"));

    JsonObject json = (JsonObject) new MultipartFormTransformer().transformRequest(null, req);
    assertThat(json).isEqualTo(expected);
  }

  @Test
  void testTransformRequestPartWithInvalidContentType() throws IOException {
    Buffer multipartBody = Buffer.buffer(new String(Files.readAllBytes(TEST_RESOURCE_PATH.resolve("multipart_part_invalid_contenttype.txt"))));
    ValidatableRequest req = mockValidatableRequest(multipartBody, "multipart/form-data; boundary=abcde12345");

    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> new MultipartFormTransformer().transformRequest(null, req));

    String expectedMsg = "The content type text/html of property id is not yet supported.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
