package io.vertx.openapi.validation.transformer;

import io.vertx.core.json.DecodeException;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaBuilder;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueTransformerTest {
  private static final ValueTransformer TRANSFORMER = new ValueTransformer() {
    @Override
    public Object transformObject(Parameter parameter, String rawValue) {
      throw new DecodeException();
    }

    @Override
    public Object transformArray(Parameter parameter, String rawValue) {
      return null;
    }
  };

  private static Parameter buildSimplePathParameter(SchemaBuilder<?, ?> schema) {
    return mockParameter("dummy", PATH, SIMPLE, false, JsonSchema.of(schema.toJson()));
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", buildSimplePathParameter(stringSchema()), "", ""),
      Arguments.of("(String) \"foobar\"", buildSimplePathParameter(stringSchema()), "foobar", "foobar"),
      Arguments.of("(Number) 14.6767", buildSimplePathParameter(numberSchema()), "14.6767", 14.6767),
      Arguments.of("(Integer) 42", buildSimplePathParameter(intSchema()), "42", 42),
      Arguments.of("(Boolean) true", buildSimplePathParameter(booleanSchema()), "true", true)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"simple\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformPrimitive(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @Test
  void testTransformDecodeException() {
    String invalidValue = "\"";
    ValidatorException exception =
      assertThrows(ValidatorException.class,
        () -> TRANSFORMER.transform(buildSimplePathParameter(objectSchema()), invalidValue));
    String expectedMsg = "The value of path parameter dummy can't be decoded.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testTransformRouting() {
    ValueTransformer vt = new ValueTransformer() {
      @Override public Object transformObject(Parameter parameter, String rawValue) {
        return "object";
      }

      @Override public Object transformPrimitive(Parameter parameter, String rawValue) {
        return "primitive";
      }

      @Override public Object transformArray(Parameter parameter, String rawValue) {
        return "array";
      }
    };

    String value = "doesn'TMatter";

    assertThat(vt.transform(buildSimplePathParameter(stringSchema()), value)).isEqualTo("primitive");
    assertThat(vt.transform(buildSimplePathParameter(numberSchema()), value)).isEqualTo("primitive");
    assertThat(vt.transform(buildSimplePathParameter(intSchema()), value)).isEqualTo("primitive");
    assertThat(vt.transform(buildSimplePathParameter(booleanSchema()), value)).isEqualTo("primitive");
    assertThat(vt.transform(buildSimplePathParameter(arraySchema()), value)).isEqualTo("array");
    assertThat(vt.transform(buildSimplePathParameter(objectSchema()), value)).isEqualTo("object");
  }
}
