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

import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.validation.ValidatorException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MultipartPart {
  private static final Pattern NAME_PATTERN = Pattern.compile("Content-Disposition: form-data; name=\"(.*?)\"",
      CASE_INSENSITIVE);
  private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("Content-Type: (.*)", CASE_INSENSITIVE);

  private static final byte[] HEADER_SECTION_DELIMITER = { '\r', '\n', '\r', '\n' };

  private final String name;
  private final String contentType;
  private final Buffer body;

  // Should only be called by MultipartFormAnalyser
  static List<MultipartPart> fromMultipartBody(Buffer body, String boundary) {
    return parseParts(body, boundary).stream().map(MultipartPart::parsePart).collect(Collectors.toList());
  }

  // VisibleForTesting
  public static List<Buffer> parseParts(Buffer body, String boundary) {
    // The parts must be split on byte level, because bodies of binary parts could contain byte sequences
    // that are invalid in UTF-8 and would get corrupted by a String round trip.
    byte[] bytes = body.getBytes();
    byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
    byte[] delimiterWithLineBreak = ("\r\n--" + boundary).getBytes(StandardCharsets.US_ASCII);

    List<Buffer> rawParts = new ArrayList<>();
    int segmentStart = 0;
    int i = 0;
    while (i < bytes.length) {
      // The line break preceding the delimiter belongs to the delimiter, not to the part body.
      byte[] match = matchesAt(bytes, i, delimiter) ? delimiter
          : matchesAt(bytes, i, delimiterWithLineBreak) ? delimiterWithLineBreak : null;
      if (match == null) {
        i++;
      } else {
        rawParts.add(body.getBuffer(segmentStart, i));
        i += match.length;
        segmentStart = i;
      }
    }
    rawParts.add(body.getBuffer(segmentStart, bytes.length));

    if (rawParts.size() < 3 || !"--".equals(rawParts.get(rawParts.size() - 1).toString().strip())) {
      String msg = "The multipart message doesn't contain any parts, or has an invalid structure.";
      throw new ValidatorException(msg, INVALID_VALUE);
    }

    List<Buffer> parts = new ArrayList<>(rawParts.size() - 2);

    // Omit first and last part, because first part is everything up to the first delimiter and the last part
    // contains "--";
    for (int j = 1; j < rawParts.size() - 1; j++) {
      parts.add(stripLeadingWhitespace(rawParts.get(j)));
    }

    return parts;
  }

  private static boolean matchesAt(byte[] bytes, int offset, byte[] pattern) {
    if (offset + pattern.length > bytes.length) {
      return false;
    }
    for (int i = 0; i < pattern.length; i++) {
      if (bytes[offset + i] != pattern[i]) {
        return false;
      }
    }
    return true;
  }

  private static Buffer stripLeadingWhitespace(Buffer rawPart) {
    int start = 0;
    while (start < rawPart.length()) {
      byte b = rawPart.getByte(start);
      if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
        start++;
      } else {
        break;
      }
    }
    return rawPart.getBuffer(start, rawPart.length());
  }

  private static Optional<String> parsePattern(Pattern pattern, String rawPart) {
    return pattern.matcher(rawPart).results().findFirst().map(m -> m.group(1));
  }

  // VisibleForTesting
  public static MultipartPart parsePart(Buffer rawPart) {
    int sectionDelimiter = -1;
    byte[] bytes = rawPart.getBytes();
    for (int i = 0; i < bytes.length; i++) {
      if (matchesAt(bytes, i, HEADER_SECTION_DELIMITER)) {
        sectionDelimiter = i;
        break;
      }
    }

    // if no empty line exists, there are only headers
    String headerSection = sectionDelimiter == -1 ? rawPart.toString()
        : rawPart.getBuffer(0, sectionDelimiter).toString();
    Buffer body = sectionDelimiter == -1 ? null
        : rawPart.getBuffer(sectionDelimiter + HEADER_SECTION_DELIMITER.length, rawPart.length());

    String name = parsePattern(NAME_PATTERN, headerSection).orElseThrow(() -> {
      String msg = "A part of the multipart message doesn't contain a name.";
      return new ValidatorException(msg, INVALID_VALUE);
    });

    // If no header is set, content type defaults to text/plain
    String contentType = parsePattern(CONTENT_TYPE_PATTERN, headerSection).orElse("text/plain");

    return new MultipartPart(name, contentType, body == null || body.length() == 0 ? null : body);
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
