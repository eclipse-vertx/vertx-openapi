package io.vertx.openapi.validation.transformer;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleTransformerTest {
  private static final Parameter DUMMY_PARAM = mockSimpleParameter("dummy", false);
  private static final Parameter DUMMY_PARAM_EXPLODE = mockSimpleParameter("dummy", true);

  private static final SimpleTransformer TRANSFORMER = new SimpleTransformer();

  private static Parameter mockSimpleParameter(String name, boolean explode) {
    return mockParameter(name, PATH, SIMPLE, explode, JsonSchema.of(stringSchema().toJson()));
  }

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

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedComplex = new JsonArray().add("Hello").add(13.37).add(1).add(false);
    return Stream.of(
      Arguments.of("empty", DUMMY_PARAM, "", EMPTY_JSON_ARRAY),
      Arguments.of("3", DUMMY_PARAM, "3", new JsonArray().add(3)),
      Arguments.of("Hello,13.37,1,false", DUMMY_PARAM, "Hello,13.37,1,false", expectedComplex)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexExplodedRaw = "string=foo,number=13.37,integer=42,boolean=true";
    String complexRaw = complexExplodedRaw.replace("=", ",");
    JsonObject expectedComplex =
      new JsonObject().put("string", "foo").put("number", 13.37).put("integer", 42).put("boolean", true);

    return Stream.of(
      Arguments.of("empty", DUMMY_PARAM, "", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", DUMMY_PARAM_EXPLODE, "", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, DUMMY_PARAM, complexRaw, expectedComplex),
      Arguments.of(complexExplodedRaw + " (exploded)", DUMMY_PARAM_EXPLODE, complexExplodedRaw,
        expectedComplex)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"simple\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformPrimitive(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"simple\" with array value: {0}")
  @MethodSource("provideValidArrayValues")
  void testTransformArrayValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformArray(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"simple\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformObject(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidValues() {
    assertThrows(DecodeException.class, () -> TRANSFORMER.transformPrimitive(DUMMY_PARAM, "\""));

    String invalidObject = "string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(DUMMY_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style simple.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
