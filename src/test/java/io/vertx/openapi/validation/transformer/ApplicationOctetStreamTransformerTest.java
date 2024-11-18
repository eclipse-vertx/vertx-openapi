/*
 * Copyright (c) 2024, Lucimber UG
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
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.vertx.openapi.MockHelper.mockValidatableRequest;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationOctetStreamTransformerTest {
  private final BodyTransformer transformer = new ApplicationOctetStreamTransformer();
  private final Random random = new Random();

  @Test
  void testTransformRequest() {
    byte[] bytes = new byte[102400]; // Mimic body of 100 Kibibyte
    random.nextBytes(bytes);
    Buffer dummyBody = Buffer.buffer(bytes);
    ValidatableRequest request = mockValidatableRequest(dummyBody, APPLICATION_OCTET_STREAM.toString());
    assertThat(transformer.transformRequest(null, request)).isEqualTo(dummyBody);
  }

  @Test
  void testTransformRequestThrows() {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> transformer.transformRequest(null,
        mockValidatableRequest(Buffer.buffer("\"foobar"), APPLICATION_JSON.toString())));
    String expectedMsg = "The request doesn't contain" +
      " the required content-type header application/octet-stream.";
    assertThat(exception.type()).isEqualTo(MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testTransformResponse() {
    byte[] bytes = new byte[102400]; // Mimic body of 100 Kibibyte
    random.nextBytes(bytes);
    Buffer dummyBody = Buffer.buffer(bytes);
    ValidatableResponse response = ValidatableResponse.create(200, dummyBody,
      APPLICATION_OCTET_STREAM.toString());
    assertThat(transformer.transformResponse(null, response)).isEqualTo(dummyBody);
  }

  @Test
  void testTransformResponseThrows() {
    ValidatableResponse response = ValidatableResponse
      .create(200, Buffer.buffer("\"foobar"), APPLICATION_JSON.toString());
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> transformer.transformResponse(null, response));
    String expectedMsg = "The response doesn't contain" +
      " the required content-type header application/octet-stream.";
    assertThat(exception.type()).isEqualTo(MISSING_REQUIRED_PARAMETER);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
