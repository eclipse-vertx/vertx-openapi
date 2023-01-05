package io.vertx.openapi.validation.validator.transformer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.LABEL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class LabelTransformerTest {
  private static final Parameter DUMMY_PARAM = mockLabelParameter("dummy", false);
  private static final Parameter DUMMY_PARAM_EXPLODE = mockLabelParameter("dummy", true);

  private static final LabelTransformer TRANSFORMER = new LabelTransformer();

  private static Parameter mockLabelParameter(String name, boolean explode) {
    return mockParameter(name, PATH, LABEL, explode, JsonSchema.of(EMPTY_JSON_OBJECT));
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", ".", ""),
      Arguments.of("(String) \".foobar\"", ".foobar", "foobar"),
      Arguments.of("(Number) .14.6767", ".14.6767", 14.6767),
      Arguments.of("(Integer) .42", ".42", 42),
      Arguments.of("(Boolean) .true", ".true", true)
    );
  }

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedNoNumber = new JsonArray().add("Hello").add(1).add(false);
    JsonArray expectedComplex = expectedNoNumber.copy().add(13.37);
    return Stream.of(
      Arguments.of("empty .", DUMMY_PARAM, ".", EMPTY_JSON_ARRAY),
      Arguments.of(".3", DUMMY_PARAM, ".3", new JsonArray().add(3)),
      Arguments.of(".3 (exploded)", DUMMY_PARAM_EXPLODE, ".3", new JsonArray().add(3)),
      Arguments.of(".Hello,1,false,13.37", DUMMY_PARAM, ".Hello,1,false,13.37", expectedComplex),
      Arguments.of(".Hello.1.false (exploded)", DUMMY_PARAM_EXPLODE, ".Hello.1.false", expectedNoNumber)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexRaw = ".string,foo,number,13.37,integer,42,boolean,true";
    String complexExplodedRaw = ".string=foo.integer=42.boolean=true";
    JsonObject expectedNoNumber =
      new JsonObject().put("string", "foo").put("integer", 42).put("boolean", true);
    JsonObject expectedComplex = expectedNoNumber.copy().put("number", 13.37);

    return Stream.of(
      Arguments.of("empty", DUMMY_PARAM, ".", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", DUMMY_PARAM_EXPLODE, ".", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, DUMMY_PARAM, complexRaw, expectedComplex),
      Arguments.of(complexExplodedRaw + " (exploded)", DUMMY_PARAM_EXPLODE, complexExplodedRaw,
        expectedNoNumber)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"label\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformPrimitive(DUMMY_PARAM, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"label\" with array value: {0}")
  @MethodSource("provideValidArrayValues")
  void testTransformArrayValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformArray(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"label\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformObject(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidValues() {
    String invalidObject = ".string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(DUMMY_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style label.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Ensure that parameter of style \"label\" start with a dot: {0}")
  @ValueSource(strings = {"", "notStartingWithDot"})
  void testTransformException(String invalidValue) {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transform(DUMMY_PARAM, invalidValue));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style label.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  @DisplayName("Ensure that values with leading dot are forwarded to the related transform methods")
  void testTransform() {
    JsonSchema schema = JsonSchema.of(arraySchema().toJson());
    Parameter param = mockParameter("dummy", PATH, LABEL, false, schema);
    LabelTransformer spyTransformer = spy(new LabelTransformer());
    spyTransformer.transform(param, ".5");
    verify(spyTransformer).transform(eq(param), eq(".5"));
  }
}
