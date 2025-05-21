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

package io.vertx.tests.mediatype;

import static com.google.common.truth.Truth.assertThat;

import io.vertx.openapi.mediatype.ContentAnalyserFactory;
import io.vertx.openapi.mediatype.impl.ApplicationJsonAnalyser;
import io.vertx.openapi.mediatype.impl.MultipartFormAnalyser;
import io.vertx.openapi.mediatype.impl.NoOpAnalyser;
import org.junit.jupiter.api.Test;

public class ContentAnalyserFactoryTest {

  @Test
  void testJson() {
    assertThat(ContentAnalyserFactory.json().create(null, null, null)).isInstanceOf(ApplicationJsonAnalyser.class);
    assertThat(ContentAnalyserFactory.noop().create(null, null, null)).isInstanceOf(NoOpAnalyser.class);
    assertThat(ContentAnalyserFactory.multipart().create(null, null, null)).isInstanceOf(MultipartFormAnalyser.class);
  }
}
