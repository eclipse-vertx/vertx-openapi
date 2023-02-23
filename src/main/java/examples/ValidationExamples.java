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

package examples;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.ValidatableResponse;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class ValidationExamples {

  private OpenAPIContract getContract() {
    return null;
  }

  private void createValidator(Vertx vertx) {
    OpenAPIContract contract = getContract();
    RequestValidator validator = RequestValidator.create(vertx, contract);

    vertx.createHttpServer().requestHandler(httpServerRequest -> {
      // Operation id must be determined for every request which is inefficient
      validator.validate(httpServerRequest).onSuccess(validatedRequest -> {
        validatedRequest.getBody(); // returns the body
        validatedRequest.getHeaders(); // returns the header
        // ..
        // ..
      });

      // Operation id will be passed to save effort for determining
      validator.validate(httpServerRequest, "yourOperationId")
        .onSuccess(validatedRequest -> {
          // do something
        });
    }).listen(0);
  }

  private ValidatableRequest getValidatableRequest() {
    return null;
  }

  private void validatableRequest(Vertx vertx) {
    OpenAPIContract contract = getContract();
    RequestValidator validator = RequestValidator.create(vertx, contract);

    ValidatableRequest request = getValidatableRequest();
    validator.validate(request, "yourOperationId").onSuccess(validatedRequest -> {
      validatedRequest.getBody(); // returns the body
      validatedRequest.getHeaders(); // returns the header
      // ..
      // ..
    });
  }

  private void validatableResponse(Vertx vertx) {
    OpenAPIContract contract = getContract();
    ResponseValidator validator = ResponseValidator.create(vertx, contract);

    JsonObject cat = new JsonObject().put("name", "foo");
    ValidatableResponse response =
      ValidatableResponse.create(200, cat.toBuffer(), APPLICATION_JSON.toString());

    vertx.createHttpServer().requestHandler(httpServerRequest -> {
      validator.validate(response, "yourOperationId")
        .onSuccess(validatedResponse -> {
          validatedResponse.getBody(); // returns the body
          validatedResponse.getHeaders(); // returns the header
          // ..
          // ..
          // send back the validated response
          validatedResponse.send(httpServerRequest.response());
        });
    });
  }
}
