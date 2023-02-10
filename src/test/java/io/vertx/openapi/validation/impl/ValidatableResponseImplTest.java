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

package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.openapi.validation.ResponseParameter;
import io.vertx.openapi.validation.ValidatableResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

class ValidatableResponseImplTest {

  @Test
  void testGetters() {
    Map<String, ResponseParameter> headers = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    ResponseParameter body = new RequestParameterImpl("param5");
    String contentType = APPLICATION_JSON.toString();
    int statusCode = 1337;

    ValidatableResponse response = new ValidatableResponseImpl(statusCode, headers, body, contentType);
    assertThat(response.getHeaders()).containsExactlyEntriesIn(headers);
    assertThat(response.getBody()).isEqualTo(body);
    assertThat(response.getContentType()).isEqualTo(contentType);
    assertThat(response.getStatusCode()).isEqualTo(statusCode);

    ValidatableResponse responseNullValues = new ValidatableResponseImpl(statusCode, null);
    assertThat(responseNullValues.getHeaders()).isEmpty();
    assertThat(responseNullValues.getBody().isEmpty()).isTrue();
    assertThat(responseNullValues.getContentType()).isNull();
    assertThat(response.getStatusCode()).isEqualTo(statusCode);
  }
}
