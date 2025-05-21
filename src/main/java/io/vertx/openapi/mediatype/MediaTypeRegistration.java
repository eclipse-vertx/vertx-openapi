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

package io.vertx.openapi.mediatype;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistration;
import io.vertx.openapi.validation.ValidationContext;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A MediaTypeRegistration is used to register mediatypes to the openapi mediatype registry. It consists of a predicate
 * that checks if a media type can be handled by the created content analysers.
 */
@VertxGen
public interface MediaTypeRegistration {

  MediaTypeRegistration APPLICATION_JSON = new DefaultMediaTypeRegistration(
      MediaTypePredicate.ofExactTypes(
          DefaultMediaTypeRegistration.APPLICATION_JSON,
          DefaultMediaTypeRegistration.APPLICATION_JSON_UTF8,
          DefaultMediaTypeRegistration.APPLICATION_HAL_JSON),
      ContentAnalyserFactory.json());

  MediaTypeRegistration MULTIPART_FORM_DATA = new DefaultMediaTypeRegistration(
      MediaTypePredicate.ofExactTypes(DefaultMediaTypeRegistration.MULTIPART_FORM_DATA),
      ContentAnalyserFactory.multipart());

  MediaTypeRegistration TEXT_PLAIN = new DefaultMediaTypeRegistration(
      MediaTypePredicate.ofExactTypes(
          DefaultMediaTypeRegistration.TEXT_PLAIN,
          DefaultMediaTypeRegistration.TEXT_PLAIN_UTF8),
      ContentAnalyserFactory.noop());

  MediaTypeRegistration APPLICATION_OCTET_STREAM = new DefaultMediaTypeRegistration(
      MediaTypePredicate.ofExactTypes(DefaultMediaTypeRegistration.APPLICATION_OCTET_STREAM),
      ContentAnalyserFactory.noop());

  MediaTypeRegistration VENDOR_SPECIFIC_JSON = new DefaultMediaTypeRegistration(
      MediaTypePredicate.ofRegexp(Pattern.compile("^[^/]+/vnd\\.[\\w.-]+\\+json$").pattern()),
      ContentAnalyserFactory.json());

  /**
   * Creates a new {@link ContentAnalyser}. This is required, because {@link ContentAnalyser} could be stateful.
   *
   * @param contentType The raw content type from the http headers.
   * @param content     The content of the request or response.
   * @param context     Whether the analyser is for a request or response.
   * @return A fresh content analyser instance.
   */
  ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context);

  /**
   * Checks if this registration can handle the given media type. This method is intended to be used by the
   * MediaTypeRegistry.
   *
   * @param mediaType The media type to check
   * @return true if the mediatype can be handled, false otherwise
   */
  boolean canHandle(String mediaType);

  /**
   * This method is intended for reporting of supported media types in the system.
   *
   * @return The list of supported types.
   */
  List<String> supportedTypes();
}
