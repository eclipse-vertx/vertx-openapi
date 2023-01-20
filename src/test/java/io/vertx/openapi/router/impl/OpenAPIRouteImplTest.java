package io.vertx.openapi.router.impl;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.router.OpenAPIRoute;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

class OpenAPIRouteImplTest {

  @Test
  void testGetters() {
    Operation mockedOperation = mock(Operation.class);
    OpenAPIRoute route = new OpenAPIRouteImpl(mockedOperation);

    assertThat(route.doValidation()).isTrue();
    assertThat(route.getOperation()).isEqualTo(mockedOperation);
    assertThat(route.getHandlers()).isEmpty();
    assertThat(route.getFailureHandlers()).isEmpty();
  }

  @Test
  void testAdders() {
    Handler<RoutingContext> dummyHandler = RoutingContext::next;
    Handler<RoutingContext> dummyFailureHandler = RoutingContext::next;
    assertThat(dummyHandler).isNotSameInstanceAs(dummyFailureHandler);

    OpenAPIRoute route = new OpenAPIRouteImpl(null);
    route.addHandler(dummyHandler);
    assertThat(route.getHandlers()).containsExactly(dummyHandler);
    route.addFailureHandler(dummyFailureHandler);
    assertThat(route.getFailureHandlers()).containsExactly(dummyFailureHandler);

    assertThat(route.doValidation()).isTrue();
    route.setDoValidation(false);
    assertThat(route.doValidation()).isFalse();
  }
}
