/*
 * Copyright (c) 2023, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

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
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.LABEL;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class LabelTransformerTest implements SchemaSupport {

  private static final Parameter OBJECT_PARAM = mockLabelParameter(OBJECT_SCHEMA, false);
  private static final Parameter ARRAY_PARAM = mockLabelParameter(ARRAY_SCHEMA, false);
  private static final Parameter STRING_PARAM = mockLabelParameter(STRING_SCHEMA, false);
  private static final Parameter NUMBER_PARAM = mockLabelParameter(NUMBER_SCHEMA, false);
  private static final Parameter INTEGER_PARAM = mockLabelParameter(INTEGER_SCHEMA, false);
  private static final Parameter BOOLEAN_PARAM = mockLabelParameter(BOOLEAN_SCHEMA, false);

  private static final Parameter OBJECT_PARAM_EXPLODE = mockLabelParameter(OBJECT_SCHEMA, true);
  private static final Parameter ARRAY_PARAM_EXPLODE = mockLabelParameter(ARRAY_SCHEMA, true);

  private static final LabelTransformer TRANSFORMER = new LabelTransformer();

  private static Parameter mockLabelParameter(JsonSchema schema, boolean explode) {
    return mockParameter(NAME, PATH, LABEL, explode, schema);
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", STRING_PARAM, ".", ""),
      Arguments.of("(String) .44", STRING_PARAM, ".44", "44"),
      Arguments.of("(String) \".foobar\"", STRING_PARAM, ".foobar", "foobar"),
      Arguments.of("(Number) .14.6767", NUMBER_PARAM, ".14.6767", 14.6767),
      Arguments.of("(Integer) .42", INTEGER_PARAM, ".42", 42),
      Arguments.of("(Boolean) .true", BOOLEAN_PARAM, ".true", true)
    );
  }

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedNoNumber = new JsonArray().add("Hello").add(1).add(false);
    JsonArray expectedComplex = expectedNoNumber.copy().add(13.37);
    return Stream.of(
      Arguments.of("empty .", ARRAY_PARAM, ".", EMPTY_JSON_ARRAY),
      Arguments.of(".3", ARRAY_PARAM, ".3", new JsonArray().add(3)),
      Arguments.of(".3 (exploded)", ARRAY_PARAM_EXPLODE, ".3", new JsonArray().add(3)),
      Arguments.of(".Hello,1,false,13.37", ARRAY_PARAM, ".Hello,1,false,13.37", expectedComplex),
      Arguments.of(".Hello.1.false (exploded)", ARRAY_PARAM_EXPLODE, ".Hello.1.false", expectedNoNumber)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexRaw = ".string,foo,number,13.37,integer,42,boolean,true";
    String complexExplodedRaw = ".string=foo.integer=42.boolean=true";
    JsonObject expectedNoNumber =
      new JsonObject().put("string", "foo").put("integer", 42).put("boolean", true);
    JsonObject expectedComplex = expectedNoNumber.copy().put("number", 13.37);

    return Stream.of(
      Arguments.of("empty", OBJECT_PARAM, ".", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", OBJECT_PARAM_EXPLODE, ".", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, OBJECT_PARAM, complexRaw, expectedComplex),
      Arguments.of(complexExplodedRaw + " (exploded)", OBJECT_PARAM_EXPLODE, complexExplodedRaw,
        expectedNoNumber)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"label\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    // Leading dot will be removed in transform method
    assertThat(TRANSFORMER.transformPrimitive(parameter, rawValue.substring(1))).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"label\" with array value: {0}")
  @MethodSource("provideValidArrayValues")
  void testTransformArrayValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    // Leading dot will be removed in transform method
    assertThat(TRANSFORMER.transformArray(parameter, rawValue.substring(1))).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"label\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    // Leading dot will be removed in transform method
    assertThat(TRANSFORMER.transformObject(parameter, rawValue.substring(1))).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidValues() {
    String invalidObject = ".string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(OBJECT_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style label.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Ensure that parameter of style \"label\" start with a dot: {0}")
  @ValueSource(strings = {"", "notStartingWithDot"})
  void testTransformException(String invalidValue) {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transform(STRING_PARAM, invalidValue));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style label.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  @DisplayName("Ensure that values with leading dot are forwarded to the related transform methods")
  void testTransform() {
    LabelTransformer spyTransformer = spy(new LabelTransformer());
    spyTransformer.transform(STRING_PARAM, ".5");
    verify(spyTransformer).transform(STRING_PARAM, ".5");
  }
}
