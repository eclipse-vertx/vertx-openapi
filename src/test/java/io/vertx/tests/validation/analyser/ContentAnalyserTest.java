/*
 * Copyright (c) 2024, SAP SE
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

import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.mediatype.impl.ApplicationJsonAnalyser;
import io.vertx.openapi.mediatype.impl.MultipartFormAnalyser;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.contract.MediaType.APPLICATION_HAL_JSON;
import static io.vertx.openapi.contract.MediaType.APPLICATION_JSON;
import static io.vertx.openapi.contract.MediaType.APPLICATION_JSON_UTF8;
import static io.vertx.openapi.contract.MediaType.MULTIPART_FORM_DATA;
import static io.vertx.openapi.mediatype.impl.AbstractContentAnalyser.getContentAnalyser;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentAnalyserTest {

  @Test
  void testGetContentAnalyser() {
    assertThat(getContentAnalyser(mockMediaType(APPLICATION_JSON), null, null, null)).isInstanceOf(ApplicationJsonAnalyser.class);
    assertThat(getContentAnalyser(mockMediaType(APPLICATION_JSON_UTF8), null, null, null)).isInstanceOf(ApplicationJsonAnalyser.class);
    assertThat(getContentAnalyser(mockMediaType(APPLICATION_HAL_JSON), null, null, null)).isInstanceOf(ApplicationJsonAnalyser.class);
    assertThat(getContentAnalyser(mockMediaType(MULTIPART_FORM_DATA), null, null, null)).isInstanceOf(MultipartFormAnalyser.class);

    assertThat(getContentAnalyser(mockMediaType("application/xml"), null, null, null)).isNull();
  }

  MediaType mockMediaType(String identifier) {
    MediaType mockedMediaType = mock(MediaType.class);
    when(mockedMediaType.getIdentifier()).thenReturn(identifier);
    return mockedMediaType;
  }
}
