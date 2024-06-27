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

import com.google.common.truth.Truth;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class RequestParameterImplTest {

  @Test
  void testGet() {
    String value = "myValue";
    Truth.assertThat(new RequestParameterImpl(value).get()).isEqualTo(value);
  }

  @Test
  void testHashcodeAndEquals() {
    RequestParameterImpl param1 = new RequestParameterImpl("param1");
    RequestParameterImpl param2 = new RequestParameterImpl("param2");

    assertThat(param1).isEqualTo(param1);
    assertThat(param1).isEqualTo(new RequestParameterImpl("param1"));
    assertThat(param1).isNotEqualTo(param2);

    assertThat(param1.hashCode()).isEqualTo(new RequestParameterImpl("param1").hashCode());
    assertThat(param1.hashCode()).isNotEqualTo(param2.hashCode());
  }
}
