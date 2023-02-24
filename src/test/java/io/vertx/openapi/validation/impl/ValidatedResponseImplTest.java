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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.test.base.HttpServerTestBase;
import io.vertx.openapi.validation.ResponseParameter;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;

class ValidatedResponseImplTest extends HttpServerTestBase {

  private ResponseValidator responseValidator;

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx, VertxTestContext testContext) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract).onSuccess(c -> {
      responseValidator = ResponseValidator.create(vertx, c);
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void initializeContract(Vertx vertx) {
    Path contractFile = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore.json");
    JsonObject contract = vertx.fileSystem().readFileBlocking(contractFile.toString()).toJsonObject();
    OpenAPIContract.from(vertx, contract);
  }

  @Test
  void testGetters() {
    Map<String, ResponseParameter> headers = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    ResponseParameter body = new RequestParameterImpl("param5");

    ValidatedResponse request = new ValidatedResponseImpl(headers, body, null);
    assertThat(request.getHeaders()).containsExactlyEntriesIn(headers);
    assertThat(request.getBody()).isEqualTo(body);
  }

  Future<?> verifyResponse(int statusCode, Buffer body, Map<String, String> headers, VertxTestContext testContext) {
    return createRequest(HttpMethod.GET, "/does_not_matter").compose(HttpClientRequest::send)
      .compose(response -> response.body().onComplete(testContext.succeeding(receivedBody -> testContext.verify(() -> {
        assertThat(response.statusCode()).isEqualTo(statusCode);
        assertThat(receivedBody).isEqualTo(body);
        assertThat(response.headers().size()).isEqualTo(headers.size());
        headers.forEach((k, v) -> assertThat(response.headers().get(k)).isEqualTo(v));
        testContext.completeNow();
      }))));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testSendNoBody(VertxTestContext testContext) {
    Map<String, String> headersExpected = ImmutableMap.of(CONTENT_LENGTH.toString(), "0");
    createServer(request -> {
      responseValidator.validate(ValidatableResponse.create(201), "createPets")
        .compose(validatedResponse -> validatedResponse.send(request.response())).onFailure(testContext::failNow);
    }).compose(v -> verifyResponse(201, Buffer.buffer(), headersExpected, testContext));
  }

  private Map<String, String> buildHeaders(Buffer body) {
    return Maps.newHashMap(ImmutableMap.of(CONTENT_TYPE.toString(), APPLICATION_JSON.toString(),
      CONTENT_LENGTH.toString(), "" + body.length()));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testSendWithBody(VertxTestContext testContext) {
    Buffer cat = new JsonObject().put("id", 1337).put("name", "foo").toBuffer();
    createServer(request -> {
      ValidatableResponse vr = ValidatableResponse.create(200, cat, APPLICATION_JSON.toString());
      responseValidator.validate(vr, "showPetById")
        .compose(validatedResponse -> validatedResponse.send(request.response())).onFailure(testContext::failNow);
    }).compose(v -> verifyResponse(200, cat, buildHeaders(cat), testContext));
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testSendWithHeaders(VertxTestContext testContext) {
    Buffer cats = new JsonArray().add(new JsonObject().put("id", 1337).put("name", "foo")).toBuffer();
    Map<String, String> headersExpected = buildHeaders(cats);
    headersExpected.put("x-next", "foo");

    createServer(request -> {
      Map<String, String> headers = ImmutableMap.of("x-next", "foo");
      ValidatableResponse vr = ValidatableResponse.create(200, headers, cats, APPLICATION_JSON.toString());
      responseValidator.validate(vr, "listPets")
        .compose(validatedResponse -> validatedResponse.send(request.response())).onFailure(testContext::failNow);
    }).compose(v -> verifyResponse(200, cats, headersExpected, testContext));
  }
}
