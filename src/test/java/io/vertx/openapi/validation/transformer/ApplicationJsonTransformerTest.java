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

package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.openapi.MockHelper.mockValidatableRequest;
import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationJsonTransformerTest {
  private final BodyTransformer transformer = new ApplicationJsonTransformer();

  @Test
  void testTransformRequest() {
    JsonObject dummyBody = new JsonObject().put("foo", "bar");
    ValidatableRequest request = mockValidatableRequest(dummyBody.toBuffer(), APPLICATION_JSON.toString());
    assertThat(transformer.transformRequest(null, request)).isEqualTo(dummyBody);
  }

  @Test
  void testTransformRequestThrows() {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> transformer.transformRequest(null,
        mockValidatableRequest(Buffer.buffer("\"foobar"), APPLICATION_JSON.toString())));
    String expectedMsg = "The request body can't be decoded";
    assertThat(exception.type()).isEqualTo(ILLEGAL_VALUE);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testTransformResponse() {
    JsonObject dummyBody = new JsonObject().put("foo", "bar");
    ValidatableResponse response = ValidatableResponse.create(200, dummyBody.toBuffer(), APPLICATION_JSON.toString());
    assertThat(transformer.transformResponse(null, response)).isEqualTo(dummyBody);
  }

  @Test
  void testTransformResponseThrows() {
    ValidatableResponse response = ValidatableResponse.create(200, Buffer.buffer("\"foobar"),
      APPLICATION_JSON.toString());

    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> transformer.transformResponse(null, response));
    String expectedMsg = "The request body can't be decoded";
    assertThat(exception.type()).isEqualTo(ILLEGAL_VALUE);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }


}
