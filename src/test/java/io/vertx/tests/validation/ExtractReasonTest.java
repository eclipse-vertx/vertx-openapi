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

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;

import com.google.common.truth.Truth;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.OutputUnit;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.SchemaValidationException;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.tests.test.base.HttpServerTestBase;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests indirectly the method {@link SchemaValidationException#extractReason(OutputUnit)},
 * there is no real unit test yet, because producing a correct mock of OutputUnit is too much effort.
 */
class ExtractReasonTest extends HttpServerTestBase {

  private final Path contractPath =
      getRelatedTestResourcePath(ExtractReasonTest.class).resolve("guestbook.yaml");
  private OpenAPIContract contract;
  private RequestValidator validator;

  private static JsonObject buildGuest(Object name, Object... friends) {
    return buildGuest(name, null, friends);
  }

  private static JsonObject buildGuest(Object name, Number age, Object... friends) {
    JsonObject guest = new JsonObject().put("name", name);
    if (age != null) {
      guest.put("age", age);
    }
    JsonArray friendsArray = new JsonArray(Arrays.asList(friends));
    if (!friendsArray.isEmpty()) {
      guest.put("friends", friendsArray);
    }
    return guest;
  }

  private static JsonObject buildEntry(Object message, Object guest) {
    return new JsonObject().put("message", message).put("guest", guest);
  }

  private static Stream<Arguments> testErrorMessage() {
    JsonObject validGuest = buildGuest("Hodor");
    JsonObject invalidGuest = buildGuest("Hodor", 13.37);
    return Stream.of(
        Arguments.of("Number for message", buildEntry(420, validGuest),
            "Reason: Instance type number is invalid. Expected string at #/message"),
        Arguments.of("Boolean for message", buildEntry(true, validGuest),
            "Reason: Instance type boolean is invalid. Expected string at #/message"),
        Arguments.of("Number for age of guest", buildEntry("msg", invalidGuest),
            "Reason: Instance type number is invalid. Expected integer at #/guest/age"),
        Arguments.of("Number for age of first friend of guest",
            buildEntry("msg", buildGuest("Hodor", 1337, invalidGuest)),
            "Reason: Instance type number is invalid. Expected integer at #/guest/friends/0/age"));
  }

  @BeforeEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void setup(Vertx vertx, VertxTestContext testContext) {
    OpenAPIContract.from(vertx, contractPath.toString()).onComplete(testContext.succeeding(c -> {
      contract = c;
      validator = RequestValidator.create(vertx, contract);
      testContext.completeNow();
    }));
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testErrorMessage(String scenario, JsonObject payload, String expected, VertxTestContext testContext) {
    createValidationHandler(
        validatorException -> Truth.assertThat(validatorException).hasMessageThat().endsWith(expected),
        testContext)
            .compose(v -> sendJson(payload.toBuffer())).onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testOutputUnit(VertxTestContext testContext) {
    JsonObject invalidGuest = buildGuest("Hodor", 13.37);
    JsonObject payload = buildEntry("msg", buildGuest("Hodor", 1337, invalidGuest));

    createValidationHandler(validatorException -> {
      Truth.assertThat(validatorException).isInstanceOf(SchemaValidationException.class);
      OutputUnit ou = ((SchemaValidationException) validatorException).getOutputUnit();
      assertThat(ou.getErrors()).hasSize(7);
      assertThat(ou.getErrors().get(6).getInstanceLocation()).isEqualTo("#/guest/friends/0/age");
    }, testContext).compose(v -> sendJson(payload.toBuffer())).onFailure(testContext::failNow);
  }

  private Future<HttpClientResponse> sendJson(Buffer json) {
    return createRequest(POST, "/bookentry").compose(req -> {
      req.putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), APPLICATION_JSON.toString());
      return req.send(json);
    });
  }

  private Future<Void> createValidationHandler(Consumer<ValidatorException> validatorException,
      VertxTestContext testContext) {
    return createServer(
        request -> validator.validate(request).onSuccess(v -> testContext.failNow("A validation error is expected"))
            .onFailure(t -> testContext.verify(() -> {
              assertThat(t).isInstanceOf(ValidatorException.class);
              ValidatorException exception = (ValidatorException) t;
              assertThat(exception.type()).isEqualTo(INVALID_VALUE);
              validatorException.accept(exception);
              testContext.completeNow();
            })));
  }
}
