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

package io.vertx.openapi.validation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.RequestBody;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.test.base.HttpServerTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.json.schema.common.dsl.SchemaType.ARRAY;
import static io.vertx.json.schema.common.dsl.SchemaType.NUMBER;
import static io.vertx.json.schema.common.dsl.SchemaType.OBJECT;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.openapi.contract.Location.COOKIE;
import static io.vertx.openapi.contract.Location.HEADER;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.Style.LABEL;
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestUtilsTest extends HttpServerTestBase {
  private static Parameter mockParameter(String name, Location in, SchemaType schemaType, boolean exploded) {
    Parameter parameter = mock(Parameter.class);
    when(parameter.getName()).thenReturn(name);
    when(parameter.getIn()).thenReturn(in);
    when(parameter.isExplode()).thenReturn(exploded);
    when(parameter.getSchemaType()).thenReturn(schemaType);
    return parameter;
  }

  private static Stream<Arguments> testExtractQuery() {
    return Stream.of(
      Arguments.of(mockParameter("foo", QUERY, NUMBER, false), "", null),
      Arguments.of(mockParameter("foo", QUERY, NUMBER, false), "foo=5.7", "5.7"),
      Arguments.of(mockParameter("foo", QUERY, ARRAY, false), "foo=3,4,5", "3,4,5"),
      Arguments.of(mockParameter("foo", QUERY, ARRAY, true), "foo=3&foo=4&bar=2", "foo=3&foo=4"),
      Arguments.of(mockParameter("foo", QUERY, OBJECT, false), "foo=name,alex,age,42", "name,alex,age,42"),
      Arguments.of(mockParameter("foo", QUERY, OBJECT, true), "foo=3&bar=2", "foo=3&bar=2")
    );
  }

  private static Stream<Arguments> testExtractCookie() {
    return Stream.of(
      Arguments.of(mockParameter("foo", COOKIE, NUMBER, false), "", null),
      Arguments.of(mockParameter("foo", COOKIE, NUMBER, false), "foo=5.7", "5.7"),
      Arguments.of(mockParameter("foo", COOKIE, ARRAY, false), "foo=3%2C4%2C5", "3,4,5"),
      // (Unsupported) Arguments.of(mockParameter("foo", COOKIE, ARRAY, true), "foo=3;foo=4;bar=2", "foo=3&foo=4"),
      Arguments.of(mockParameter("foo", COOKIE, OBJECT, false), "foo=name%2Calex%2Cage%2C42;", "name,alex,age,42"),
      Arguments.of(mockParameter("foo", COOKIE, OBJECT, true), "bar=2;foo=3", "bar=2&foo=3")
    );
  }

  private static Stream<Arguments> testExtractHeader() {
    return Stream.of(
      Arguments.of(mockParameter("foo", HEADER, NUMBER, false), ImmutableMap.of(), null),
      Arguments.of(mockParameter("foo", HEADER, NUMBER, false), ImmutableMap.of("foo", "5.7"), "5.7"),
      Arguments.of(mockParameter("foo", HEADER, ARRAY, false), ImmutableMap.of("foo", "3,4,5"), "3,4,5"),
      Arguments.of(mockParameter("foo", HEADER, ARRAY, true), ImmutableMap.of("foo", "3,4,5"), "3,4,5"),
      Arguments.of(mockParameter("foo", HEADER, OBJECT, false), ImmutableMap.of("foo", "name,alex,age,42"),
        "name,alex,age,42"),
      Arguments.of(mockParameter("foo", HEADER, OBJECT, true), ImmutableMap.of("foo", "name=alex,age=42"),
        "name=alex,age=42")
    );
  }

  private static Stream<Arguments> testExtractPath() {
    return Stream.of(
      // Primitive Simple
      Arguments.of(mockParameter("foo", PATH, NUMBER, false), SIMPLE, "", null),
      Arguments.of(mockParameter("foo", PATH, NUMBER, false), SIMPLE, "5.7", "5.7"),
      // Primitive Label
      Arguments.of(mockParameter("foo", PATH, NUMBER, false), LABEL, ".", "."),
      Arguments.of(mockParameter("foo", PATH, NUMBER, false), LABEL, ".5.7", ".5.7"),
      // Primitive Matrix
      Arguments.of(mockParameter("foo", PATH, NUMBER, false), MATRIX, ";foo", ";foo"),
      Arguments.of(mockParameter("foo", PATH, NUMBER, false), MATRIX, ";foo=5.7", ";foo=5.7"),

      // Array Simple
      Arguments.of(mockParameter("foo", PATH, ARRAY, false), SIMPLE, "3,4,5", "3,4,5"),
      Arguments.of(mockParameter("foo", PATH, ARRAY, true), SIMPLE, "3,4,5", "3,4,5"),
      // Array Label
      Arguments.of(mockParameter("foo", PATH, ARRAY, false), LABEL, ".3,4,5", ".3,4,5"),
      Arguments.of(mockParameter("foo", PATH, ARRAY, true), LABEL, ".3.4.5", ".3.4.5"),
      // Array Matrix
      Arguments.of(mockParameter("foo", PATH, ARRAY, false), MATRIX, ";foo=3,4,5", ";foo=3,4,5"),
      Arguments.of(mockParameter("foo", PATH, ARRAY, true), MATRIX, ";foo=3;foo=4;foo=5", ";foo=3;foo=4;foo=5"),

      // Object Simple
      Arguments.of(mockParameter("foo", PATH, OBJECT, false), SIMPLE, "name,alex,age,42", "name,alex,age,42"),
      Arguments.of(mockParameter("foo", PATH, OBJECT, true), SIMPLE, "name=alex,age=42", "name=alex,age=42"),
      // Object Label
      Arguments.of(mockParameter("foo", PATH, OBJECT, false), LABEL, ".name,alex,age,42", ".name,alex,age,42"),
      Arguments.of(mockParameter("foo", PATH, OBJECT, true), LABEL, ".name=alex.age=42", ".name=alex.age=42"),
      // Object Label
      Arguments.of(mockParameter("foo", PATH, OBJECT, false), MATRIX, ":foo=,name,alex,age,42",
        ":foo=,name,alex,age,42"),
      Arguments.of(mockParameter("foo", PATH, OBJECT, true), MATRIX, ";name=alex;age=42", ";name=alex;age=42")
    );
  }

  private static Stream<Arguments> testFindPathSegment() {
    return Stream.of(
      Arguments.of("/{foo}", "foo", 1),
      Arguments.of("/test/{foo}", "foo", 2),
      Arguments.of("/test/{foo}/user/{bar}", "foo", 2),
      Arguments.of("/test/{foo}/user/{bar}", "bar", 4),
      Arguments.of("/test/{foo}/{bar}", "bar", 3),
      Arguments.of("/{foo}/{bar}/test", "bar", 2)
    );
  }

  @ParameterizedTest(name = "{index} Query {1} should be transformed into {2}")
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testExtractQuery(Parameter parameter, String query, String expected, VertxTestContext testContext) {
    createValidationHandler(params -> {
      assertThat(params.getQuery().get(parameter.getName()).getString()).isEqualTo(expected);
      testContext.completeNow();
    }, mockOperation(parameter), testContext).compose(
        v -> createRequest(HttpMethod.GET, "?" + query).map(HttpClientRequest::send))
      .onFailure(testContext::failNow);
  }

  @ParameterizedTest(name = "{index} Cookies should be transformed into {2}")
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testExtractCookie(Parameter parameter, String cookieString, String expected, VertxTestContext testContext) {
    createValidationHandler(params -> {
      assertThat(params.getCookies().get(parameter.getName()).getString()).isEqualTo(expected);
      testContext.completeNow();
    }, mockOperation(parameter), testContext).compose(
        v -> createRequest(HttpMethod.GET, "").map(req -> req.putHeader("Cookie", cookieString).send()))
      .onFailure(testContext::failNow);
  }

  @ParameterizedTest(name = "{index} Header should be transformed into {2}")
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testExtractHeader(Parameter parameter, Map<String, String> headers, String expected,
    VertxTestContext testContext) {
    createValidationHandler(params -> {
      assertThat(params.getHeaders().get(parameter.getName()).getString()).isEqualTo(expected);
      testContext.completeNow();
    }, mockOperation(parameter), testContext).compose(v ->
      createRequest(HttpMethod.GET, "").map(req -> {
        headers.forEach(req::putHeader);
        return req.send();
      })
    ).onFailure(testContext::failNow);
  }

  @ParameterizedTest(name = "{index} Path {2} ({1}) should be transformed into {3}")
  @MethodSource
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testExtractPath(Parameter parameter, Style style, String path, String expected, VertxTestContext testContext) {
    Operation mockedOperation = mockOperation(parameter);
    when(mockedOperation.getOpenAPIPath()).thenReturn("/test/{foo}");

    createValidationHandler(params -> {
      assertThat(params.getPathParameters().get(parameter.getName()).getString()).isEqualTo(expected);
      testContext.completeNow();
    }, mockedOperation, testContext).compose(
        v -> createRequest(HttpMethod.GET, "/test/" + path + "/").map(HttpClientRequest::send))
      .onFailure(testContext::failNow);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testExtractBody(VertxTestContext testContext) {

    JsonObject bodyJson = new JsonObject().put("foo", "bar");

    createValidationHandler(params -> {
      assertThat(params.getContentType()).isEqualTo(APPLICATION_JSON.toString());
      assertThat(params.getBody().isBuffer()).isTrue();
      assertThat(params.getBody().getBuffer().toJsonObject()).isEqualTo(bodyJson);
      testContext.completeNow();
    }, mockOperationWithSimpleRequestBody(), testContext).compose(v -> createRequest(HttpMethod.POST, ""))
      .map(req -> req.putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
        .send(bodyJson.toBuffer()))
      .onFailure(testContext::failNow);
  }

  @ParameterizedTest(name = "{index} Template path {0} has parameter {1} in the {2} section")
  @MethodSource
  void testFindPathSegment(String templatePath, String parameterName, int expected) {
    assertThat(RequestUtils.findPathSegment(templatePath, parameterName)).isEqualTo(expected);
  }

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void testBodySupplier(VertxTestContext testContext) {
    Checkpoint firstRead = testContext.checkpoint();
    Checkpoint secondRead = testContext.checkpoint();
    Checkpoint thirdRead = testContext.checkpoint();

    Operation operation = mockOperationWithSimpleRequestBody();
    JsonObject firstBody = new JsonObject().put("foo", "bar");
    JsonObject secondBody = new JsonObject().put("foo", "bar");

    createServer(request -> RequestUtils.extract(request, operation).compose(validatableRequest -> {
      testContext.verify(() -> {
        assertThat(validatableRequest.getBody().getBuffer().toJsonObject()).isEqualTo(firstBody);
        firstRead.flag();
      });
      return RequestUtils.extract(request, operation).recover(t -> {
        testContext.verify(() -> {
          assertThat(t).isInstanceOf(IllegalStateException.class);
          assertThat(t).hasMessageThat().isEqualTo("Request has already been read");
          secondRead.flag();
        });
        return RequestUtils.extract(request, operation, () -> Future.succeededFuture(secondBody.toBuffer()));
      });
    }).onSuccess(validatableRequest -> {
      testContext.verify(() -> {
        assertThat(validatableRequest.getBody().getBuffer().toJsonObject()).isEqualTo(secondBody);
        thirdRead.flag();
      });
      request.response().send().onFailure(testContext::failNow);
    })).compose(v -> createRequest(HttpMethod.POST, ""))
      .map(req -> req.putHeader(HttpHeaderNames.CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
        .send(firstBody.toBuffer()))
      .onFailure(testContext::failNow);
  }

  private Future<Void> createValidationHandler(Consumer<ValidatableRequest> verifier, Operation operation,
    VertxTestContext testContext) {

    return createServer(request -> RequestUtils.extract(request, operation)
      .onComplete(testContext.succeeding(validatableRequest -> testContext.verify(() -> {
        verifier.accept(validatableRequest);
        request.response().send().onFailure(testContext::failNow);
      }))));
  }

  private Operation mockOperation(Parameter parameter) {
    return mockOperation(ImmutableList.of(parameter));
  }

  private Operation mockOperation(List<Parameter> parameters) {
    Operation op = mock(Operation.class);
    when(op.getParameters()).thenReturn(parameters);
    return op;
  }

  private Operation mockOperationWithSimpleRequestBody() {
    MediaType mockedMediaType = mock(MediaType.class);
    when(mockedMediaType.getSchema()).thenReturn(JsonSchema.of(objectSchema().toJson()));

    RequestBody mockedRequestBody = mock(RequestBody.class);
    when(mockedRequestBody.isRequired()).thenReturn(true);
    when(mockedRequestBody.getContent()).thenReturn(ImmutableMap.of(APPLICATION_JSON.toString(), mockedMediaType));

    Operation mockedOperation = mockOperation(emptyList());
    when(mockedOperation.getRequestBody()).thenReturn(mockedRequestBody);

    return mockedOperation;
  }
}
