package io.vertx.openapi.validation.transformer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.COOKIE;
import static io.vertx.openapi.contract.Style.FORM;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormTransformerTest {
  private static final Parameter DUMMY_PARAM = mockFormParameter("dummy", false);
  private static final Parameter DUMMY_PARAM_EXPLODE = mockFormParameter("dummy", true);
  private static final FormTransformer TRANSFORMER = new FormTransformer();

  private static Parameter mockFormParameter(String name, boolean explode) {
    return mockParameter(name, COOKIE, FORM, explode, JsonSchema.of(stringSchema().toJson()));
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", "", ""),
      Arguments.of("(String) \"foobar\"", "foobar", "foobar"),
      Arguments.of("(Number) 14.6767", "14.6767", 14.6767),
      Arguments.of("(Integer) 42", "42", 42),
      Arguments.of("(Boolean) true", "true", true)
    );
  }

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedComplex = new JsonArray().add("Hello").add(1).add(false).add(13.37);
    return Stream.of(
      Arguments.of("empty", DUMMY_PARAM, "", EMPTY_JSON_ARRAY),
      Arguments.of("3", DUMMY_PARAM, "3", new JsonArray().add(3)),
      Arguments.of("dummy=3 (exploded)", DUMMY_PARAM_EXPLODE, "dummy=3", new JsonArray().add(3)),
      Arguments.of("Hello,1,false,13.37", DUMMY_PARAM, "Hello,1,false,13.37", expectedComplex),
      Arguments.of("dummy=Hello&dummy=1&dummy=false&dummy=13.37 (exploded)", DUMMY_PARAM_EXPLODE,
        "dummy=Hello&dummy=1&dummy=false&dummy=13.37", expectedComplex)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexRaw = "string,foo,number,13.37,integer,42,boolean,true";
    String complexExplodedRaw = "string=foo&number=13.37&integer=42&boolean=true";
    JsonObject expected =
      new JsonObject().put("string", "foo").put("integer", 42).put("boolean", true).put("number", 13.37);

    return Stream.of(
      Arguments.of("empty", DUMMY_PARAM, "", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", DUMMY_PARAM_EXPLODE, "", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, DUMMY_PARAM, complexRaw, expected),
      Arguments.of(complexExplodedRaw + " (exploded)", DUMMY_PARAM_EXPLODE, complexExplodedRaw, expected)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Cookie\" parameter of style \"from\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformPrimitive(DUMMY_PARAM, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Cookie\" parameter of style \"from\" with array value: {0}")
  @MethodSource("provideValidArrayValues")
  void testTransformArrayValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformArray(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Cookie\" parameter of style \"from\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformObject(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidValues() {
    String invalidObject = "string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(DUMMY_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of cookie parameter dummy doesn't match to style form.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
