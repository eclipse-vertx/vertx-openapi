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

package io.vertx.openapi.test;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.ContractOptions;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.test.base.HttpServerTestBase;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static io.vertx.openapi.ResourceHelper.getRelatedTestResourcePath;

class E2ETest extends HttpServerTestBase {

  private OpenAPIContract contract;
  private RequestValidator requestValidator;
  private ResponseValidator responseValidator;

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void setup(VertxTestContext testContext) {
    String contractPah = getRelatedTestResourcePath(E2ETest.class).resolve("petstore.json").toString();
    ContractOptions options = new ContractOptions().setBasePath("/base/");
    OpenAPIContract.from(vertx, contractPah, options).onComplete(testContext.succeeding(contract -> {
      this.contract = contract;
      this.requestValidator = RequestValidator.create(vertx, contract);
      this.responseValidator = ResponseValidator.create(vertx, contract);
      testContext.completeNow();
    }));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testExtractPath(VertxTestContext testContext) {
    Operation operation = contract.operation("showPetById");
    int expectedPetId = 1;
    JsonObject expectedPet = new JsonObject().put("id", 1).put("name", "FooBar");

    createValidationHandler(req -> {
      testContext.verify(() -> {
        int petId = req.getPathParameters().get("petId").getInteger();
        assertThat(petId).isEqualTo(expectedPetId);
      });
      return ValidatableResponse.create(200, expectedPet.toBuffer(), APPLICATION_JSON);
    }, operation.getOperationId(), testContext)

      .compose(v -> {
        Future<HttpClientRequest> req = createRequest(HttpMethod.GET, "/base/pets/" + expectedPetId);
        return request(req, resp -> resp.body().onComplete(testContext.succeeding(
          body -> testContext.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(body.toJsonObject()).isEqualTo(expectedPet);
            testContext.completeNow();
          }))), testContext);
      }).onFailure(testContext::failNow);
  }

  private Future<Void> request(Future<HttpClientRequest> request, Consumer<HttpClientResponse> verifier,
    VertxTestContext testContext) {
    return request.compose(HttpClientRequest::send).onSuccess(response -> {
      testContext.verify(() -> verifier.accept(response));
    }).onFailure(testContext::failNow).mapEmpty();
  }

  private Future<Void> createValidationHandler(Function<ValidatedRequest, ValidatableResponse> processor,
    String operationId, VertxTestContext testContext) {
    return createServer(request -> {
      requestValidator.validate(request, operationId)
        .map(validatedRequest -> processor.apply(validatedRequest))
        .compose(validatableResponse -> responseValidator.validate(validatableResponse, operationId))
        .compose(validatedResponse -> validatedResponse.send(request.response())).onFailure(testContext::failNow);
    }).onFailure(testContext::failNow);
  }
}
