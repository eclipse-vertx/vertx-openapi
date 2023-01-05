package io.vertx.openapi.validation;

import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.impl.RequestValidatorImpl;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestValidatorTest {

  @Test
  void testCreate() {
    OpenAPIContract contract = mock(OpenAPIContract.class);
    when(contract.getSchemaRepository()).thenReturn(mock(SchemaRepository.class));
    assertThat(RequestValidator.create(null, contract)).isInstanceOf(RequestValidatorImpl.class);
  }
}
