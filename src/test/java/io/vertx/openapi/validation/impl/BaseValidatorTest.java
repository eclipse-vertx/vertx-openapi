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

package io.vertx.openapi.validation.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static org.mockito.Mockito.spy;

@ExtendWith(VertxExtension.class)
class BaseValidatorTest {
  private BaseValidator validator;

  private OpenAPIContract contractSpy;

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract).onSuccess(c -> testContext.verify(() -> {
      this.contractSpy = spy(c);
      this.validator = new BaseValidator(vertx, contractSpy);
      testContext.completeNow();
    })).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testGetOperation(VertxTestContext testContext) {
    validator.getOperation("invalidId").onFailure(t -> testContext.verify(() -> {
      assertThat(t).isInstanceOf(ValidatorException.class);
      assertThat(t).hasMessageThat().isEqualTo("Invalid OperationId: invalidId");
      testContext.completeNow();
    })).onSuccess(v -> testContext.failNow("Test expects a failure"));
  }

  @ParameterizedTest(name = "{index} Test valid if {0} is a valid base transformer.")
  @ValueSource(strings = { "application/json", "application/hal+json" })
  public void testValidBaseTransformer(String transformer) {
    assertThat(validator.bodyTransformers.containsKey(transformer)).isTrue();
  }

}
