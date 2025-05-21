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

package io.vertx.openapi.mediatype;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.openapi.validation.ValidatorException;

/**
 * The content analyser is responsible to check if a request or response has the correct format i.e. is syntactically
 * correct, and to transform the content-buffer into a representation that can be validated against a schema.
 * <p>
 * These two methods are intentionally bundled in {@link ContentAnalyser} to prevent some operations from having to
 * be performed twice. This is particularly helpful if a library is used that cannot distinguish between these steps.
 * In this case, an intermediate result that was generated in {@link #checkSyntacticalCorrectness()}, for example,
 * can be reused.
 * <p>
 * Therefore, it is very important to ensure that the {@link #checkSyntacticalCorrectness()} method is always called
 * before.
 */
@VertxGen
public interface ContentAnalyser {
  /**
   * Checks if the content has the expected format i.e. is syntactically correct.
   * <p>
   * Throws a {@link ValidatorException} if the content is syntactically incorrect.
   */
  void checkSyntacticalCorrectness() throws ValidatorException;

  /**
   * Transforms the content into a format that can be validated by the
   * {@link io.vertx.openapi.validation.RequestValidator}, or {@link io.vertx.openapi.validation.ResponseValidator}.
   * <p>
   * Throws a {@link ValidatorException} if the content can't be transformed.
   *
   * @return the transformed content.
   */
  Object transform();
}
