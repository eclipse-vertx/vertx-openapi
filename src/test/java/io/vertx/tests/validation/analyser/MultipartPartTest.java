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

package io.vertx.tests.validation.analyser;

import com.google.common.truth.Truth;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatorErrorType;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.mediatype.impl.MultipartPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipartPartTest {
  private static final Path TEST_RESOURCE_PATH = getRelatedTestResourcePath(MultipartPartTest.class);

  @Test
  void testParseParts() throws IOException {
    String part1 = Files.readString(TEST_RESOURCE_PATH.resolve("part1.txt"));
    String part2 = Files.readString(TEST_RESOURCE_PATH.resolve("part2.txt"));

    String multipartBody = Files.readString(TEST_RESOURCE_PATH.resolve("multipart.txt"));
    Truth.assertThat(MultipartPart.parseParts(multipartBody, "abcde12345")).containsExactly(part1, part2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"multipart_invalid_structure", "multipart_invalid_structure_2"})
  void testParsePartsInvalidStructure(String file) throws IOException {
    String multipartBody = Files.readString(TEST_RESOURCE_PATH.resolve(file + ".txt"));

    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> MultipartPart.parseParts(multipartBody, "abcde12345"));

    String expectedMsg = "The multipart message doesn't contain any parts, or has an invalid structure.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.INVALID_VALUE);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testParsePart() throws IOException {
    String part1 = Files.readString(TEST_RESOURCE_PATH.resolve("part1.txt"));
    MultipartPart mpp1 = MultipartPart.parsePart(part1);
    assertThat(mpp1.getName()).isEqualTo("id");
    assertThat(mpp1.getContentType()).isEqualTo("text/plain");
    assertThat(mpp1.getBody()).isEqualTo(Buffer.buffer("123e4567-e89b-12d3-a456-426655440000"));

    String part2 = Files.readString(TEST_RESOURCE_PATH.resolve("part2.txt"));
    MultipartPart mpp2 = MultipartPart.parsePart(part2);
    assertThat(mpp2.getName()).isEqualTo("address");
    assertThat(mpp2.getContentType()).isEqualTo("application/json");
    JsonObject body = new JsonObject()
      .put("street", "3, Garden St")
      .put("city", "Hillsbery, UT");
    assertThat(mpp2.getBody().toJsonObject()).isEqualTo(body);

    String part3 = Files.readString(TEST_RESOURCE_PATH.resolve("part3.txt"));
    MultipartPart mpp3 = MultipartPart.parsePart(part3);
    assertThat(mpp3.getName()).isEqualTo("randomBinary");
    assertThat(mpp3.getContentType()).isEqualTo("application/octet-stream");
    assertThat(mpp3.getBody()).isEqualTo(Buffer.buffer("9*\u00914±Ê\u0006-\u009Bå"));
  }

  @Test
  void testParsePartWithoutName() throws IOException {
    String part = Files.readString(TEST_RESOURCE_PATH.resolve("part_without_name.txt"));

    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> MultipartPart.parsePart(part));

    String expectedMsg = "A part of the multipart message doesn't contain a name.";
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.INVALID_VALUE);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testParsePartWithoutContentType() throws IOException {
    String part = Files.readString(TEST_RESOURCE_PATH.resolve("part_without_contenttype.txt"));

    MultipartPart mpp = MultipartPart.parsePart(part);
    assertThat(mpp.getName()).isEqualTo("id");
    assertThat(mpp.getContentType()).isEqualTo("text/plain");
    assertThat(mpp.getBody()).isEqualTo(Buffer.buffer("123e4567-e89b-12d3-a456-426655440000"));
  }

  @Test
  void testParsePartWithoutBody() throws IOException {
    String part = Files.readString(TEST_RESOURCE_PATH.resolve("part_without_body.txt"));
    MultipartPart mpp = MultipartPart.parsePart(part);
    assertThat(mpp.getName()).isEqualTo("id");
    assertThat(mpp.getContentType()).isEqualTo("text/plain");
    assertThat(mpp.getBody()).isNull();
  }
}
