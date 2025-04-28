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

package io.vertx.tests.test;

import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.tests.test.base.ContractTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;

class PhoneNumberTest extends ContractTestBase {
  private Path CONTRACT_FILE = getRelatedTestResourcePath(PhoneNumberTest.class)
    .resolve("contract_various_scenarios.yaml");

  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @Test
  @DisplayName("Test that the request content type check is less restrictive")
  void testPhoneNumberInQuery(VertxTestContext testContext) {
    String operationId = "phoneNumberInQuery";
    String queryParam = "phoneNumber";
    String queryValue = "+901200";

    loadContract(CONTRACT_FILE, testContext).compose(v -> createServerWithRequestProcessor(req -> {
        assertThat(req.getQuery().get(queryParam).getString()).isEqualTo(queryValue);
        req.getQuery().get(queryParam).getString();
        testContext.completeNow();
        return ValidatableResponse.create(200);
      }, operationId, testContext))
      .compose(v -> createRequest(HttpMethod.POST, "/phoneNumber?" + queryParam + "=" + queryValue))
      .compose(request -> request.send())
      .onFailure(testContext::failNow);
  }
}
