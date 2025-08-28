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

package io.vertx.tests.validation;

import static com.google.common.truth.Truth.assertThat;

import io.vertx.openapi.validation.ValidationContext;
import org.junit.jupiter.api.Test;

public class ValidationContextTest {

  @Test
  public void testValidationContext() {
    assertThat(ValidationContext.REQUEST.toString()).isEqualTo("request");
    assertThat(ValidationContext.RESPONSE.toString()).isEqualTo("response");
  }
}
