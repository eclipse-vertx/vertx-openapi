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

package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.mediatype.ContentAnalyser;
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;

/**
 * Base class for your own analysers. Provides access to some useful helper methods.
 */
public abstract class AbstractContentAnalyser implements ContentAnalyser {

  /**
   * Returns the content analyser for the given content type.
   *
   * @param mediaType   the media type to determine the content analyser.
   * @param contentType the raw content type value from the HTTP header field.
   * @param content     the content to be analysed.
   * @return the content analyser for the given content type.
   */
  public static AbstractContentAnalyser getContentAnalyser(MediaType mediaType, String contentType, Buffer content,
                                                           ValidationContext context) {
    switch (mediaType.getIdentifier()) {
      case MediaType.APPLICATION_JSON:
      case MediaType.APPLICATION_JSON_UTF8:
      case MediaType.APPLICATION_HAL_JSON:
        return new ApplicationJsonAnalyser(contentType, content, context);
      case MediaType.MULTIPART_FORM_DATA:
        return new MultipartFormAnalyser(contentType, content, context);
      case MediaType.APPLICATION_OCTET_STREAM:
      case MediaType.TEXT_PLAIN:
      case MediaType.TEXT_PLAIN_UTF8:
        return new NoOpAnalyser(contentType, content, context);
      default:
        if (MediaType.isVendorSpecificJson(contentType)) {
          return new ApplicationJsonAnalyser(contentType, content, context);
        }
        return null;
    }
  }

  protected final String contentType;
  protected final Buffer content;
  protected final ValidationContext requestOrResponse;

  /**
   * Creates a new content analyser.
   *
   * @param contentType the content type.
   * @param content     the content to be analysed.
   * @param context     the context in which the content is used.
   */
  public AbstractContentAnalyser(String contentType, Buffer content, ValidationContext context) {
    this.contentType = contentType;
    this.content = content;
    this.requestOrResponse = context;
  }

  /**
   * Builds a {@link ValidatorException} for the case that the content is syntactically incorrect.
   *
   * @param message the error message.
   * @return the {@link ValidatorException}.
   */
  protected static ValidatorException buildSyntaxException(String message) {
    return new ValidatorException(message, ILLEGAL_VALUE);
  }

  /**
   * Decodes the passed content as JSON.
   *
   * @return an object representing the passed JSON content.
   * @throws ValidatorException if the content can't be decoded.
   */
  protected static Object decodeJsonContent(Buffer content, ValidationContext requestOrResponse) {
    try {
      return Json.decodeValue(content);
    } catch (DecodeException e) {
      throw buildSyntaxException("The " + requestOrResponse + " body can't be decoded");
    }
  }
}
