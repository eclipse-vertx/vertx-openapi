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

package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.validation.ValidationContext;

public class NoOpAnalyser extends AbstractContentAnalyser {
  public NoOpAnalyser(String contentType, Buffer content, ValidationContext context) {
    super(contentType, content, context);
  }

  @Override
  public void checkSyntacticalCorrectness() {
    // no syntax check
  }

  @Override
  public Object transform() {
    return content;
  }
}
