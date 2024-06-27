package io.vertx.tests.validation.transformer;

import io.vertx.core.json.DecodeException;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.transformer.ParameterTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.tests.MockHelper.mockParameter;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ParameterTransformerTest {
  private ParameterTransformer TRANSFORMER;

  private static Parameter buildSimplePathParameter(SchemaBuilder<?, ?> schema) {
    return mockParameter("dummy", PATH, SIMPLE, false, JsonSchema.of(schema.toJson()));
  }

  @BeforeEach
  void setUp() {
    TRANSFORMER = new ParameterTransformer() {
      @Override
      protected String[] getArrayValues(Parameter parameter, String rawValue) {
        return new String[0];
      }

      @Override
      protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
        return new String[0];
      }

      @Override
      public Object transformObject(Parameter parameter, String rawValue) {
        return "object";
      }

      @Override
      public Object transformPrimitive(Parameter parameter, String rawValue) {
        return "primitive";
      }

      @Override
      public Object transformArray(Parameter parameter, String rawValue) {
        return "array";
      }
    };
  }

  @Test
  void testTransformDecodeException() {
    TRANSFORMER = spy(TRANSFORMER);
    when(TRANSFORMER.transformObject(any(), any())).thenThrow(DecodeException.class);
    String invalidValue = "\"";
    ValidatorException exception =
      assertThrows(ValidatorException.class,
        () -> TRANSFORMER.transform(buildSimplePathParameter(objectSchema()), invalidValue));
    String expectedMsg = "The value of path parameter dummy can't be decoded.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testTransformRouting() {
    String value = "doesn'TMatter";

    assertThat(TRANSFORMER.transform(buildSimplePathParameter(stringSchema()), value)).isEqualTo("primitive");
    assertThat(TRANSFORMER.transform(buildSimplePathParameter(numberSchema()), value)).isEqualTo("primitive");
    assertThat(TRANSFORMER.transform(buildSimplePathParameter(intSchema()), value)).isEqualTo("primitive");
    assertThat(TRANSFORMER.transform(buildSimplePathParameter(booleanSchema()), value)).isEqualTo("primitive");
    assertThat(TRANSFORMER.transform(buildSimplePathParameter(arraySchema()), value)).isEqualTo("array");
    assertThat(TRANSFORMER.transform(buildSimplePathParameter(objectSchema()), value)).isEqualTo("object");
  }
}
