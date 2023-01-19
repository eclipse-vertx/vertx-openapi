package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatableRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

class ValidatableRequestImplTest {

  @Test
  void testGetters() {
    Map<String, RequestParameter> cookies = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> headers = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> path = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> query = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    RequestParameter body = new RequestParameterImpl("param5");
    String contentType = APPLICATION_JSON.toString();

    ValidatableRequest request = new ValidatableRequestImpl(cookies, headers, path, query, body, contentType);
    assertThat(request.getHeaders()).containsExactlyEntriesIn(headers);
    assertThat(request.getCookies()).containsExactlyEntriesIn(cookies);
    assertThat(request.getPathParameters()).containsExactlyEntriesIn(path);
    assertThat(request.getQuery()).containsExactlyEntriesIn(query);
    assertThat(request.getBody()).isEqualTo(body);
    assertThat(request.getContentType()).isEqualTo(contentType);

    ValidatableRequest requestNullValues = new ValidatableRequestImpl(null, null, null, null);
    assertThat(requestNullValues.getHeaders()).isEmpty();
    assertThat(requestNullValues.getCookies()).isEmpty();
    assertThat(requestNullValues.getPathParameters()).isEmpty();
    assertThat(requestNullValues.getQuery()).isEmpty();
    assertThat(requestNullValues.getBody().isEmpty()).isTrue();
    assertThat(requestNullValues.getContentType()).isNull();
  }
}
