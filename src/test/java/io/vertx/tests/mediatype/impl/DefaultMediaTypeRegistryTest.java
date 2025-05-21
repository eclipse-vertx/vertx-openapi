/*
 * Copyright (c) 2025, SAP SE
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

import io.vertx.openapi.mediatype.MediaTypeRegistration;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistration;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistry;
import org.junit.jupiter.api.Test;

class DefaultMediaTypeRegistryTest {

  @Test
  void testGet() {
    DefaultMediaTypeRegistry registry = new DefaultMediaTypeRegistry();
    MediaTypeRegistration jsonReg = MediaTypeRegistration.APPLICATION_JSON;
    MediaTypeRegistration textReg = MediaTypeRegistration.TEXT_PLAIN;
    registry.register(jsonReg).register(textReg);

    assertThat(registry.get("application/json")).isSameInstanceAs(jsonReg);
    assertThat(registry.get("text/plain")).isSameInstanceAs(textReg);
    assertThat(registry.get("foo/bar")).isNull();
  }

  @Test
  void testSupportedTypes() {
    DefaultMediaTypeRegistry registry = new DefaultMediaTypeRegistry();
    MediaTypeRegistration jsonReg = MediaTypeRegistration.APPLICATION_JSON;
    MediaTypeRegistration textReg = MediaTypeRegistration.TEXT_PLAIN;
    registry.register(jsonReg).register(textReg);
    assertThat(registry.supportedTypes()).containsExactly(
        DefaultMediaTypeRegistration.APPLICATION_JSON,
        DefaultMediaTypeRegistration.APPLICATION_JSON_UTF8,
        DefaultMediaTypeRegistration.APPLICATION_HAL_JSON,
        DefaultMediaTypeRegistration.TEXT_PLAIN,
        DefaultMediaTypeRegistration.TEXT_PLAIN_UTF8);
  }
}
