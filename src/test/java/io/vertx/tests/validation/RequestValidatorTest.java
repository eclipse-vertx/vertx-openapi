/*
 * Copyright (c) 2023, SAP SE
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

import com.google.common.truth.Truth;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.impl.RequestValidatorImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestValidatorTest {

  @Test
  void testCreate() {
    OpenAPIContract contract = mock(OpenAPIContract.class);
    when(contract.getSchemaRepository()).thenReturn(mock(SchemaRepository.class));
    Truth.assertThat(RequestValidator.create(null, contract)).isInstanceOf(RequestValidatorImpl.class);
  }
}
