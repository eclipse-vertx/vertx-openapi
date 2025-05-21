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
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.ValidatorException;

public class ApplicationJsonAnalyser extends AbstractContentAnalyser {
  private Object decodedValue;

  public ApplicationJsonAnalyser(String contentType, Buffer content, ValidationContext context) {
    super(contentType, content, context);
  }

  @Override
  public void checkSyntacticalCorrectness() {
    decodedValue = decodeJsonContent(content, requestOrResponse);
  }

  @Override
  public Object transform() {
    return decodedValue;
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
