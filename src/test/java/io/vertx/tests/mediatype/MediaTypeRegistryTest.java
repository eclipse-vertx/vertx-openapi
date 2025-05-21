/*
 * Copyright (c) 2025, Lukas Jelonek
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.tests.mediatype;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.mediatype.MediaTypePredicate.ofExactTypes;

import io.vertx.openapi.mediatype.ContentAnalyserFactory;
import io.vertx.openapi.mediatype.MediaTypeRegistration;
import io.vertx.openapi.mediatype.MediaTypeRegistry;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistration;
import org.junit.jupiter.api.Test;

public class MediaTypeRegistryTest {

  @Test
  void testCreateEmpty() {
    MediaTypeRegistry r = MediaTypeRegistry.createEmpty();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.TEXT_PLAIN)).isFalse();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.TEXT_PLAIN_UTF8)).isFalse();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_JSON)).isFalse();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_JSON_UTF8)).isFalse();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_HAL_JSON)).isFalse();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_OCTET_STREAM)).isFalse();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.MULTIPART_FORM_DATA)).isFalse();
  }

  @Test
  void testCreateDefault() {
    MediaTypeRegistry r = MediaTypeRegistry.createDefault();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.TEXT_PLAIN)).isTrue();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.TEXT_PLAIN_UTF8)).isTrue();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_JSON)).isTrue();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_JSON_UTF8)).isTrue();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_HAL_JSON)).isTrue();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.APPLICATION_OCTET_STREAM)).isTrue();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.MULTIPART_FORM_DATA)).isTrue();
  }

  @Test
  void testIsSupported() {
    MediaTypeRegistry r = MediaTypeRegistry.createEmpty();
    assertThat(r.isSupported(DefaultMediaTypeRegistration.TEXT_PLAIN)).isFalse();
    r.register(MediaTypeRegistration.TEXT_PLAIN);
    assertThat(r.isSupported(DefaultMediaTypeRegistration.TEXT_PLAIN)).isTrue();
  }

  @Test
  void addCustomTypeShouldMakeItSupported() {
    MediaTypeRegistry r = MediaTypeRegistry.createEmpty();
    String t = "application/vnd.openxmlformats-officedocument.drawingml.diagramData+xml";
    r.register(new DefaultMediaTypeRegistration(ofExactTypes(t), ContentAnalyserFactory.noop()));
    assertThat(r.isSupported(t)).isTrue();
  }
}
