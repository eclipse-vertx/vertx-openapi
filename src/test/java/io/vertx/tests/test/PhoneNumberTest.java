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

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.tests.test.base.ContractTestBase;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhoneNumberTest extends ContractTestBase {
  private Path CONTRACT_FILE = getRelatedTestResourcePath(PhoneNumberTest.class)
      .resolve("contract_various_scenarios.yaml");

  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @Test
  @DisplayName("Test that query parameters don't get decoded twice and loose a '+' sign")
  void testPhoneNumberInQuery(VertxTestContext testContext) {
    String operationId = "phoneNumberInQuery";
    String queryParam = "phoneNumber";
    String queryValue = "+39 334 192 1829";
    String encodedQueryValue = URLEncoder.encode(queryValue, StandardCharsets.UTF_8);
    String queryString = queryParam + "=" + encodedQueryValue;

    Function<ValidatedRequest, ValidatableResponse> requestProcessor = req -> {
      assertThat(req.getQuery().get(queryParam).getString()).isEqualTo(queryValue);
      return ValidatableResponse.create(200);
    };

    Consumer<HttpClientResponse> responseVerifier = resp -> testContext.verify(() -> {
      assertThat(resp.statusCode()).isEqualTo(200);
      testContext.completeNow();
    });

    loadContract(CONTRACT_FILE, testContext)
        .compose(v -> createServerWithRequestProcessor(requestProcessor, operationId, testContext))
        .compose(v -> {
          Future<HttpClientRequest> req = createRequest(POST, "/phoneNumber?" + queryString);
          return sendAndVerifyRequest(req, responseVerifier, testContext);
        }).onFailure(testContext::failNow);
  }

  // @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @Test
  @DisplayName("Test that force encoding of header and cookies is respected")
  void testForceEncodingHeaderAndCookies(VertxTestContext testContext) {
    String operationId = "forceEncoding";
    String headerEncoded = "headerEncoded";
    String headerNotEncoded = "headerNotEncoded";
    String cookieEncoded = "cookieEncoded";
    String cookieNotEncoded = "cookieNotEncoded";
    String encodedTestValue = "3%2C4%2C5";
    String decodedTestValue = "3,4,5";

    Function<ValidatedRequest, ValidatableResponse> requestProcessor = req -> {
      assertThat(req.getCookies().get(cookieEncoded).getString()).isEqualTo(encodedTestValue);
      assertThat(req.getCookies().get(cookieNotEncoded).getString()).isEqualTo(decodedTestValue);
      assertThat(req.getHeaders().get(headerEncoded).getString()).isEqualTo(encodedTestValue);
      assertThat(req.getHeaders().get(headerNotEncoded).getString()).isEqualTo(decodedTestValue);
      return ValidatableResponse.create(200);
    };

    Consumer<HttpClientResponse> responseVerifier = resp -> testContext.verify(() -> {
      assertThat(resp.statusCode()).isEqualTo(200);
      testContext.completeNow();
    });

    loadContract(CONTRACT_FILE, testContext)
        .compose(v -> createServerWithRequestProcessor(requestProcessor, operationId, testContext))
        .compose(v -> {
          Future<HttpClientRequest> req = createRequest(POST, "/phoneNumber", reqOpts -> {
            reqOpts.putHeader(headerEncoded, encodedTestValue);
            reqOpts.putHeader(headerNotEncoded, encodedTestValue);
            reqOpts.putHeader(HttpHeaders.COOKIE, cookieEncoded + "=" + encodedTestValue + "; "
                + cookieNotEncoded + "=" + encodedTestValue);
          });

          return sendAndVerifyRequest(req, responseVerifier, testContext);
        }).onFailure(testContext::failNow);
  }
}
