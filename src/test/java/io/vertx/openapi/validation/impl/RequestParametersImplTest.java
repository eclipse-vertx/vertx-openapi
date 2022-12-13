package io.vertx.openapi.validation.impl;

import com.google.common.collect.ImmutableMap;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.RequestParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

class RequestParametersImplTest {

  @Test
  void testGetters() {
    Map<String, RequestParameter> cookies = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> headers = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> path = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    Map<String, RequestParameter> query = ImmutableMap.of("param1", new RequestParameterImpl("Param1"));
    RequestParameter body = new RequestParameterImpl("param5");

    RequestParameters params = new RequestParametersImpl(cookies, headers, path, query, body);
    assertThat(params.getHeaders()).containsExactlyEntriesIn(headers);
    assertThat(params.getCookies()).containsExactlyEntriesIn(cookies);
    assertThat(params.getPathParameters()).containsExactlyEntriesIn(path);
    assertThat(params.getQuery()).containsExactlyEntriesIn(query);
    assertThat(params.getBody()).isEqualTo(body);
  }
}
