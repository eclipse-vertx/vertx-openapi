package io.vertx.openapi.validation.impl;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class RequestParameterImplTest {

  @Test
  void testGet() {
    String value = "myValue";
    assertThat(new RequestParameterImpl(value).get()).isEqualTo(value);
  }

  @Test
  void testhashcodeAndEquals() {
    RequestParameterImpl param1 = new RequestParameterImpl("param1");
    RequestParameterImpl param2 = new RequestParameterImpl("param2");

    assertThat(param1).isEqualTo(param1);
    assertThat(param1).isEqualTo(new RequestParameterImpl("param1"));
    assertThat(param1).isNotEqualTo(param2);

    assertThat(param1.hashCode()).isEqualTo(new RequestParameterImpl("param1").hashCode());
    assertThat(param1.hashCode()).isNotEqualTo(param2.hashCode());
  }
}
