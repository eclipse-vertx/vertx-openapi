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
import static org.mockito.Mockito.*;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.ContentAnalyser;
import io.vertx.openapi.mediatype.ContentAnalyserFactory;
import io.vertx.openapi.mediatype.MediaTypeInfo;
import io.vertx.openapi.mediatype.MediaTypePredicate;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistration;
import io.vertx.openapi.validation.ValidationContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultMediaTypeRegistrationTest {

  private MediaTypePredicate predicate;
  private ContentAnalyserFactory factory;
  private DefaultMediaTypeRegistration registration;

  @BeforeEach
  void setUp() {
    predicate = mock(MediaTypePredicate.class);
    factory = mock(ContentAnalyserFactory.class);
    registration = new DefaultMediaTypeRegistration(predicate, factory);
  }

  @Test
  void testCreateContentAnalyser() {
    String contentType = "application/json";
    Buffer buffer = mock(Buffer.class);
    ValidationContext context = mock(ValidationContext.class);
    ContentAnalyser analyser = mock(ContentAnalyser.class);
    when(factory.create(contentType, buffer, context)).thenReturn(analyser);

    ContentAnalyser result = registration.createContentAnalyser(contentType, buffer, context);
    assertThat(result).isSameInstanceAs(analyser);
    verify(factory).create(contentType, buffer, context);
  }

  @Test
  void testCanHandle() {
    String mediaType = "application/xml";
    when(predicate.test(any(MediaTypeInfo.class))).thenReturn(true);

    assertThat(registration.canHandle(mediaType)).isTrue();
    verify(predicate).test(any(MediaTypeInfo.class));
  }

  @Test
  void testSupportedTypes() {
    List<String> types = Arrays.asList("application/json", "application/xml");
    when(predicate.supportedTypes()).thenReturn(types);

    List<String> result = registration.supportedTypes();
    assertThat(result).isEqualTo(types);
    verify(predicate).supportedTypes();
  }
}
