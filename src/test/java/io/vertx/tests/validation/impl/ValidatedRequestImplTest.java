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

package io.vertx.tests.validation.impl;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import io.vertx.openapi.validation.impl.ValidatedRequestImpl;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidatedRequestImplTest {
  @Test
  void testGetters() {
    Map<String, RequestParameter> cookies = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> headers = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> path = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> query = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    RequestParameter body = new RequestParameterImpl("param5");

    ValidatedRequest request = new ValidatedRequestImpl(cookies, headers, path, query, body);
    assertThat(request.getHeaders()).containsExactlyEntriesIn(headers);
    assertThat(request.getCookies()).containsExactlyEntriesIn(cookies);
    assertThat(request.getPathParameters()).containsExactlyEntriesIn(path);
    assertThat(request.getQuery()).containsExactlyEntriesIn(query);
    assertThat(request.getBody()).isEqualTo(body);

    ValidatedRequest requestNullValues = new ValidatedRequestImpl(null, null, null, null);
    assertThat(requestNullValues.getHeaders()).isEmpty();
    assertThat(requestNullValues.getCookies()).isEmpty();
    assertThat(requestNullValues.getPathParameters()).isEmpty();
    assertThat(requestNullValues.getQuery()).isEmpty();
    assertThat(requestNullValues.getBody().isEmpty()).isTrue();
  }
}
