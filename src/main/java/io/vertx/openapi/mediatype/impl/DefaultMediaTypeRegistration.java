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

package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.ContentAnalyser;
import io.vertx.openapi.mediatype.ContentAnalyserFactory;
import io.vertx.openapi.mediatype.MediaTypeInfo;
import io.vertx.openapi.mediatype.MediaTypePredicate;
import io.vertx.openapi.mediatype.MediaTypeRegistration;
import io.vertx.openapi.validation.ValidationContext;
import java.util.List;

public class DefaultMediaTypeRegistration implements MediaTypeRegistration {
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF8 = APPLICATION_JSON + "; charset=utf-8";
  public static final String MULTIPART_FORM_DATA = "multipart/form-data";
  public static final String APPLICATION_HAL_JSON = "application/hal+json";
  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_PLAIN_UTF8 = TEXT_PLAIN + "; charset=utf-8";

  private final MediaTypePredicate canHandleMediaType;
  private final ContentAnalyserFactory contentAnalyserFactory;

  /**
   * Creates a new registration from the provided predicate and ContentAnalyserFactory.
   *
   * @param canHandleMediaType A predicate to check if the mediatype can be handled
   * @return The registration object
   */
  public DefaultMediaTypeRegistration(MediaTypePredicate canHandleMediaType,
      ContentAnalyserFactory contentAnalyserFactory) {
    this.canHandleMediaType = canHandleMediaType;
    this.contentAnalyserFactory = contentAnalyserFactory;
  }

  @Override
  public ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context) {
    return contentAnalyserFactory.create(contentType, content, context);
  }

  @Override
  public boolean canHandle(String mediaType) {
    return canHandleMediaType.test(MediaTypeInfo.of(mediaType));
  }

  @Override
  public List<String> supportedTypes() {
    return canHandleMediaType.supportedTypes();
  }
}
