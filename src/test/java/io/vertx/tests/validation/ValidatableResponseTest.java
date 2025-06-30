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

package io.vertx.tests.validation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatableResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidatableResponseTest {

  private final int dummyStatusCode = 1337;

  private final Map<String, String> dummyHeaders = ImmutableMap.of("foo", "bar");

  private final Buffer dummyBody = new JsonObject().put("name", "foo").toBuffer();

  private final String dummyContentType = HttpHeaderValues.APPLICATION_JSON.toString();

  @Test
  void testCreateStatusCode() {
    ValidatableResponse response = ValidatableResponse.create(dummyStatusCode);
    assertThat(response.getStatusCode()).isEqualTo(dummyStatusCode);
    assertThat(response.getHeaders()).isEmpty();
    assertThat(response.getContentType()).isNull();
    assertThat(response.getBody().get()).isNull();
  }

  @Test
  void testCreateStatusCodeAndHeaders() {
    ValidatableResponse response = ValidatableResponse.create(dummyStatusCode, dummyHeaders);
    assertThat(response.getStatusCode()).isEqualTo(dummyStatusCode);
    assertThat(response.getHeaders()).hasSize(1);
    assertThat(response.getContentType()).isNull();
    assertThat(response.getBody().get()).isNull();
  }

  @Test
  void testCreateStatusCodeAndBody() {
    ValidatableResponse response = ValidatableResponse.create(dummyStatusCode, dummyBody, dummyContentType);
    assertThat(response.getStatusCode()).isEqualTo(dummyStatusCode);
    assertThat(response.getHeaders()).isEmpty();
    assertThat(response.getBody().get()).isEqualTo(dummyBody);
    assertThat(response.getContentType()).isEqualTo(dummyContentType);
  }

  @Test
  void testCreate() {
    ValidatableResponse response =
        ValidatableResponse.create(dummyStatusCode, dummyHeaders, dummyBody, dummyContentType);
    assertThat(response.getStatusCode()).isEqualTo(dummyStatusCode);
    assertThat(response.getHeaders()).hasSize(1);
    assertThat(response.getBody().get()).isEqualTo(dummyBody);
    assertThat(response.getContentType()).isEqualTo(dummyContentType);
  }

  @Test
  void testCreateThrows() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> ValidatableResponse.create(dummyStatusCode, dummyHeaders, dummyBody, null));
    assertThat(exception).hasMessageThat().isEqualTo("When a body is passed, the content type MUST be specified");
  }
}
