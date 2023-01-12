package io.vertx.openapi.validation.transformer;

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
import static io.vertx.openapi.contract.Style.MATRIX;
import static io.vertx.openapi.validation.transformer.MatrixTransformer.removePrefix;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class MatrixTransformerTest {
  private static final Parameter DUMMY_PARAM = mockMatrixParameter("dummy", false);
  private static final Parameter DUMMY_PARAM_EXPLODE = mockMatrixParameter("dummy", true);

  private static final MatrixTransformer TRANSFORMER = new MatrixTransformer();

  private static Parameter mockMatrixParameter(String name, boolean explode) {
    return mockParameter(name, PATH, MATRIX, explode, JsonSchema.of(EMPTY_JSON_OBJECT));
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", ";dummy=", ""),
      Arguments.of("(String) \";dummy=foobar\"", ";dummy=foobar", "foobar"),
      Arguments.of("(Number) ;dummy=14.6767", ";dummy=14.6767", 14.6767),
      Arguments.of("(Integer) ;dummy=42", ";dummy=42", 42),
      Arguments.of("(Boolean) ;dummy=true", ";dummy=true", true)
    );
  }

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedComplex = new JsonArray().add("Hello").add(1).add(false).add(13.37);
    return Stream.of(
      Arguments.of("empty ;dummy=", DUMMY_PARAM, ";dummy=", EMPTY_JSON_ARRAY),
      Arguments.of(";dummy=3", DUMMY_PARAM, ";dummy=3", new JsonArray().add(3)),
      Arguments.of(";dummy=3 (exploded)", DUMMY_PARAM_EXPLODE, ";dummy=3", new JsonArray().add(3)),
      Arguments.of(";dummy=Hello,1,false,13.37", DUMMY_PARAM, ";dummy=Hello,1,false,13.37", expectedComplex),
      Arguments.of(";dummy=Hello;dummy=1;dummy=false;dummy=13.37 (exploded)", DUMMY_PARAM_EXPLODE,
        ";dummy=Hello;dummy=1;dummy=false;dummy=13.37", expectedComplex)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexRaw = ";dummy=string,foo,number,13.37,integer,42,boolean,true";
    String complexExplodedRaw = ";string=foo;number=13.37;integer=42;boolean=true";
    JsonObject expected =
      new JsonObject().put("string", "foo").put("integer", 42).put("boolean", true).put("number", 13.37);

    return Stream.of(
      Arguments.of("empty", DUMMY_PARAM, ";dummy=", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", DUMMY_PARAM_EXPLODE, ";", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, DUMMY_PARAM, complexRaw, expected),
      Arguments.of(complexExplodedRaw + " (exploded)", DUMMY_PARAM_EXPLODE, complexExplodedRaw, expected)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"matrix\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformPrimitive(DUMMY_PARAM, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"matrix\" with array value: {0}")
  @MethodSource("provideValidArrayValues")
  void testTransformArrayValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformArray(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"matrix\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformObject(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidValues() {
    String invalidObject = ";dummy=string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(DUMMY_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style matrix.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Ensure that parameter of style \"matrix\" start with a ;parameterName=: {0}")
  @ValueSource(strings = {"", ";dummy", ";foo="})
  void testTransformException(String invalidValue) {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transform(DUMMY_PARAM, invalidValue));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style matrix.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  @DisplayName("Ensure that values with leading matrix prefix are forwarded to the related transform methods")
  void testTransform() {
    JsonSchema schema = JsonSchema.of(arraySchema().toJson());
    Parameter param = mockParameter("dummy", PATH, MATRIX, false, schema);
    MatrixTransformer spyTransformer = spy(new MatrixTransformer());
    spyTransformer.transform(param, ";dummy=5");
    verify(spyTransformer).transform(param, ";dummy=5");
  }

  @Test
  void testRemovePrefix() {
    assertThat(removePrefix(DUMMY_PARAM, "")).isEqualTo("");
    assertThat(removePrefix(DUMMY_PARAM, ";dummy=")).isEqualTo("");
  }
}
