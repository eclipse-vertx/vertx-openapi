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

package io.vertx.tests.test.base;

import static io.vertx.openapi.impl.Utils.readYamlOrJson;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedRequest;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

// We need to enforce sequential execution, because the tests are manipulating fields
@Execution(ExecutionMode.SAME_THREAD)
public class ContractTestBase extends HttpServerTestBase {
  protected OpenAPIContract contract;
  protected RequestValidator requestValidator;
  protected ResponseValidator responseValidator;

  /**
   * Loads the contract and initialize request and response validators.
   *
   * @param contractFile the contract file
   * @param testContext  the test context
   * @return a future that will be completed when the contract is loaded
   */
  protected Future<Void> loadContract(Path contractFile, VertxTestContext testContext) {
    return loadContract(contractFile, "", testContext);
  }

  /**
   * Loads the contract, initialize request and response validators and allows to modify the base path.
   *
   * @param contractFile the contract file
   * @param basePath     the base path to be set in the contract
   * @param testContext  the test context
   * @return a future that will be completed when the contract is loaded
   */
  protected Future<Void> loadContract(Path contractFile, String basePath, VertxTestContext testContext) {
    return readYamlOrJson(vertx, contractFile.toAbsolutePath().toString()).compose(contractJson -> {
      JsonObject server = contractJson.getJsonArray("servers").getJsonObject(0);
      server.put("url", server.getString("url") + basePath);
      return OpenAPIContract.from(vertx, contractJson).onComplete(testContext.succeeding(contract -> {
        this.contract = contract;
        this.requestValidator = RequestValidator.create(vertx, contract);
        this.responseValidator = ResponseValidator.create(vertx, contract);
      }));
    }).mapEmpty();
  }

  /**
   * Creates the HTTP Server and adds a global request handler that validates the request and response. It's a global
   * handler, so only one endpoint can be tested per server creation.
   *
   * @param processor   allows to process the validated request and create and return a validatable response
   * @param operationId the operation id of the request
   * @param testContext the test context
   * @return a future that will be completed when the server is created
   */
  protected Future<Void> createServerWithRequestProcessor(Function<ValidatedRequest, ValidatableResponse> processor,
      String operationId, VertxTestContext testContext) {
    return createServer(request -> requestValidator.validate(request, operationId)
        .map(processor).compose(validatableResponse -> responseValidator.validate(validatableResponse, operationId))
        .compose(validatedResponse -> validatedResponse.send(request.response()))
        .onFailure(testContext::failNow)).onFailure(testContext::failNow);
  }

  /**
   * Sends the request and allows to verify the response, if the request fails the test context automatically fails.
   *
   * @param request     the request to be sent
   * @param verifier    the verifier that will be called with the response
   * @param testContext the test context
   * @return a future that will be completed when the request was sent and the response was verified
   */
  protected Future<Void> sendAndVerifyRequest(Future<HttpClientRequest> request, Consumer<HttpClientResponse> verifier,
      VertxTestContext testContext) {
    return request.compose(HttpClientRequest::send)
        .onSuccess(response -> testContext.verify(() -> verifier.accept(response))).onFailure(testContext::failNow)
        .mapEmpty();
  }
}
