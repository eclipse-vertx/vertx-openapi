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

package io.vertx.tests.test;

import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.tests.test.base.HttpServerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.tests.ResourceHelper.loadJson;

// We need to enforce sequential execution, because the tests are manipulating fields
@Execution(ExecutionMode.SAME_THREAD)
class E2ETest extends HttpServerTestBase {

  private OpenAPIContract contract;
  private RequestValidator requestValidator;
  private ResponseValidator responseValidator;

  Future<Void> setupContract(String basePath, VertxTestContext testContext) {
    JsonObject contractJson = loadJson(vertx, getRelatedTestResourcePath(E2ETest.class).resolve("petstore.json"));
    // Modify base path
    JsonObject server = contractJson.getJsonArray("servers").getJsonObject(0);
    server.put("url", server.getString("url") + basePath);
    return OpenAPIContract.from(vertx, contractJson).onComplete(testContext.succeeding(contract -> {
      this.contract = contract;
      this.requestValidator = RequestValidator.create(vertx, contract);
      this.responseValidator = ResponseValidator.create(vertx, contract);
    })).mapEmpty();
  }

  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @ParameterizedTest(name = "{index} Test with base path: {0}")
  @ValueSource(strings = {"", "/base", "/base/"})
  void testExtractPath(String basePath, VertxTestContext testContext) {
    int expectedPetId = 1;
    JsonObject expectedPet = new JsonObject().put("id", 1).put("name", "FooBar");

    setupContract(basePath, testContext).compose(v -> createValidationHandler(req -> {
      testContext.verify(() -> {
        int petId = req.getPathParameters().get("petId").getInteger();
        assertThat(petId).isEqualTo(expectedPetId);
      });
      return ValidatableResponse.create(200, expectedPet.toBuffer(), HttpHeaderValues.APPLICATION_JSON.toString());
    }, contract.operation("showPetById").getOperationId(), testContext)).compose(v -> {
      String bp = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
      Future<HttpClientRequest> req = createRequest(HttpMethod.GET, bp + "/pets/" + expectedPetId);
      return request(req, resp -> resp.body().onComplete(testContext.succeeding(
        body -> testContext.verify(() -> {
          assertThat(resp.statusCode()).isEqualTo(200);
          assertThat(body.toJsonObject()).isEqualTo(expectedPet);
          testContext.completeNow();
        }))), testContext);
    }).onFailure(testContext::failNow);
  }

  //These types come from: https://spec.openapis.org/oas/latest.html#special-considerations-for-multipart-content
  // 1 primitive "petId"
  // 1 complex "json"
  // 1 primitive with content encoding aka png base64 encoded.
  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @DisplayName("Send a multipart/form-data request")
  public void sendMultipartFormDataRequest(VertxTestContext testContext) {
    Path path = getRelatedTestResourcePath(E2ETest.class).resolve("multipart.txt");
    JsonObject expectedPetMetadata = new JsonObject()
      .put("friends", new JsonArray().add(123).add(456).add(789))
      .put("contactInformation", new JsonObject()
        .put("name", "Example")
        .put("email", "example@example.com")
        .put("phone", "5555555555"));

    setupContract("", testContext).compose(v -> createValidationHandler(req -> {
        testContext.verify(() -> {
          assertThat(req.getBody()).isNotNull();
          JsonObject jsonReq = req.getBody().getJsonObject();
          assertThat(jsonReq.getLong("petId")).isEqualTo(1234L);
          assertThat(jsonReq.getJsonObject("petMetadata")).isEqualTo(expectedPetMetadata);
          assertThat(jsonReq.getBuffer("petPicture")).isNotNull();
          testContext.completeNow();
        });
        return ValidatableResponse.create(201);
      }, contract.operation("uploadPet").getOperationId(), testContext))
      .compose(v -> createRequest(HttpMethod.POST, "/pets/upload"))
      .map(request -> request.putHeader(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=4ad8accc990e99c2"))
      .map(request -> request.putHeader(HttpHeaders.CONTENT_DISPOSITION, ""))
      .compose(request -> request.send(vertx.fileSystem().readFileBlocking(path.toString())))
      .onFailure(testContext::failNow);
  }

  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @ParameterizedTest(name = "{index} Request with content type {0} passes validation")
  @ValueSource(strings = {"application/json", "application/json; charset=utf-8"})
  @DisplayName("Test that the request content type check is less restrictive")
  void testLessRestrictiveContentType(String requestContentType, VertxTestContext testContext) {
    JsonObject expectedPet = new JsonObject().put("id", 1).put("name", "FooBar");

    setupContract("", testContext).compose(v -> createValidationHandler(req -> {
        testContext.completeNow();
        return ValidatableResponse.create(201);
      }, contract.operation("createPets").getOperationId(), testContext))
      .compose(v -> createRequest(HttpMethod.POST, "/pets"))
      .map(request -> request.putHeader(HttpHeaders.CONTENT_TYPE, requestContentType))
      .compose(request -> request.send(expectedPet.toBuffer()))
      .onFailure(testContext::failNow);
  }

  private Future<Void> request(Future<HttpClientRequest> request, Consumer<HttpClientResponse> verifier,
                               VertxTestContext testContext) {
    return request.compose(HttpClientRequest::send)
      .onSuccess(response -> testContext.verify(() -> verifier.accept(response))).onFailure(testContext::failNow)
      .mapEmpty();
  }

  private Future<Void> createValidationHandler(Function<ValidatedRequest, ValidatableResponse> processor,
                                               String operationId, VertxTestContext testContext) {
    return createServer(request -> requestValidator.validate(request, operationId)
      .map(processor).compose(validatableResponse -> responseValidator.validate(validatableResponse, operationId))
      .compose(validatedResponse -> validatedResponse.send(request.response()))
      .onFailure(testContext::failNow)).onFailure(testContext::failNow);
  }
}
