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
import io.vertx.openapi.validation.ValidatorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class MultipartPart {
  private static final Pattern NAME_PATTERN = Pattern.compile("Content-Disposition: form-data; name=\"(.*?)\"",
    CASE_INSENSITIVE);
  private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("Content-Type: (.*)", CASE_INSENSITIVE);

  private final String name;
  private final String contentType;
  private final Buffer body;

  // Should only be called by MultipartPartFormTransformer
  static List<MultipartPart> fromMultipartBody(String body, String boundary) {
    return parseParts(body, boundary).stream().map(MultipartPart::parsePart).collect(Collectors.toList());
  }

  // VisibleForTesting
  public static List<String> parseParts(String body, String boundary) {
    String delimiter = "--" + boundary + "|\r\n--" + boundary;
    String[] rawParts = body.split(delimiter);

    if (rawParts.length < 3 || !"--".equals(rawParts[rawParts.length - 1].strip())) {
      String msg = "The multipart message doesn't contain any parts, or has an invalid structure.";
      throw new ValidatorException(msg, INVALID_VALUE);
    }

    List<String> parts = new ArrayList<>(rawParts.length - 2);

    // Omit first and last part, because first part is everything up to the first delimiter and the last part
    // contains "--";
    for (int i = 1; i < rawParts.length - 1; i++) {
      parts.add(rawParts[i].strip());
    }

    return parts;
  }

  private static Optional<String> parsePattern(Pattern pattern, String rawPart) {
    return pattern.matcher(rawPart).results().findFirst().map(m -> m.group(1));
  }

  // VisibleForTesting
  public static MultipartPart parsePart(String rawPart) {
    String sectionDelimiterPattern = "\r\n\r\n";
    int sectionDelimiter = rawPart.indexOf(sectionDelimiterPattern);

    // if no empty line exists, there are only headers
    String headerSection = sectionDelimiter == -1 ? rawPart : rawPart.substring(0, sectionDelimiter);
    String body = sectionDelimiter == -1 ? null :
      rawPart.substring(sectionDelimiter + sectionDelimiterPattern.length());

    String name = parsePattern(NAME_PATTERN, headerSection).orElseThrow(() -> {
      String msg = "A part of the multipart message doesn't contain a name.";
      return new ValidatorException(msg, INVALID_VALUE);
    });

    // If no header is set, content type defaults to text/plain
    String contentType = parsePattern(CONTENT_TYPE_PATTERN, headerSection).orElse("text/plain");

    return new MultipartPart(name, contentType, body == null ? null : Buffer.buffer(body));
  }

  public MultipartPart(String name, String contentType, Buffer body) {
    this.name = name;
    this.contentType = contentType;
    this.body = body;
  }

  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }

  public Buffer getBody() {
    return body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MultipartPart that = (MultipartPart) o;
    return Objects.equals(name, that.name) && Objects.equals(contentType, that.contentType) && Objects.equals(body,
      that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, contentType, body);
  }
}
