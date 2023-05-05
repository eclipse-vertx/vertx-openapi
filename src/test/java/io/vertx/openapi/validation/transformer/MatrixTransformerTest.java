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
import static io.vertx.json.schema.common.dsl.Schemas.arraySchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.MATRIX;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class MatrixTransformerTest implements SchemaSupport {

  private static final Parameter OBJECT_PARAM = mockMatrixParameter(OBJECT_SCHEMA, false);
  private static final Parameter ARRAY_PARAM = mockMatrixParameter(ARRAY_SCHEMA, false);
  private static final Parameter STRING_PARAM = mockMatrixParameter(STRING_SCHEMA, false);
  private static final Parameter NUMBER_PARAM = mockMatrixParameter(NUMBER_SCHEMA, false);
  private static final Parameter INTEGER_PARAM = mockMatrixParameter(INTEGER_SCHEMA, false);
  private static final Parameter BOOLEAN_PARAM = mockMatrixParameter(BOOLEAN_SCHEMA, false);

  private static final Parameter OBJECT_PARAM_EXPLODE = mockMatrixParameter(OBJECT_SCHEMA, true);
  private static final Parameter ARRAY_PARAM_EXPLODE = mockMatrixParameter(ARRAY_SCHEMA, true);

  private static final MatrixTransformer TRANSFORMER = new MatrixTransformer();

  private static Parameter mockMatrixParameter(JsonSchema schema, boolean explode) {
    return mockParameter(NAME, PATH, MATRIX, explode, schema);
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", STRING_PARAM, ";dummy=", ""),
      Arguments.of("(String) \";dummy=foobar\"", STRING_PARAM, ";dummy=foobar", "foobar"),
      Arguments.of("(Number) ;dummy=14.6767", NUMBER_PARAM, ";dummy=14.6767", 14.6767),
      Arguments.of("(Integer) ;dummy=42", INTEGER_PARAM, ";dummy=42", 42),
      Arguments.of("(Boolean) ;dummy=true", BOOLEAN_PARAM, ";dummy=true", true)
    );
  }

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedComplex = new JsonArray().add("Hello").add(1).add(false).add(13.37);
    return Stream.of(
      Arguments.of("empty ;dummy=", ARRAY_PARAM, ";dummy=", EMPTY_JSON_ARRAY),
      Arguments.of(";dummy=3", ARRAY_PARAM, ";dummy=3", new JsonArray().add(3)),
      Arguments.of(";dummy=3 (exploded)", ARRAY_PARAM_EXPLODE, ";dummy=3", new JsonArray().add(3)),
      Arguments.of(";dummy=Hello,1,false,13.37", ARRAY_PARAM, ";dummy=Hello,1,false,13.37", expectedComplex),
      Arguments.of(";dummy=Hello;dummy=1;dummy=false;dummy=13.37 (exploded)", ARRAY_PARAM_EXPLODE,
        ";dummy=Hello;dummy=1;dummy=false;dummy=13.37", expectedComplex)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexRaw = ";dummy=string,foo,number,13.37,integer,42,boolean,true";
    String complexExplodedRaw = ";string=foo;number=13.37;integer=42;boolean=true";
    JsonObject expected =
      new JsonObject().put("string", "foo").put("integer", 42).put("boolean", true).put("number", 13.37);

    return Stream.of(
      Arguments.of("empty", OBJECT_PARAM, ";dummy=", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", OBJECT_PARAM_EXPLODE, ";", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, OBJECT_PARAM, complexRaw, expected),
      Arguments.of(complexExplodedRaw + " (exploded)", OBJECT_PARAM_EXPLODE, complexExplodedRaw, expected)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"matrix\" with primitive value: {0}")
  @MethodSource("provideValidPrimitiveValues")
  void testTransformPrimitiveValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    // Leading prefix will be removed in transform method
    int prefixLength = TRANSFORMER.buildPrefix(parameter).length();
    assertThat(TRANSFORMER.transformPrimitive(parameter, rawValue.substring(prefixLength))).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"matrix\" with array value: {0}")
  @MethodSource("provideValidArrayValues")
  void testTransformArrayValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    // Leading prefix will be removed in transform method
    int prefixLength = TRANSFORMER.buildPrefix(parameter).length();
    assertThat(TRANSFORMER.transformArray(parameter, rawValue.substring(prefixLength))).isEqualTo(expectedValue);
  }

  @ParameterizedTest(name = "{index} Transform \"Path\" parameter of style \"matrix\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    // Leading prefix will be removed in transform method
    int prefixLength = TRANSFORMER.buildPrefix(parameter).length();
    assertThat(TRANSFORMER.transformObject(parameter, rawValue.substring(prefixLength))).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidValues() {
    String invalidObject = ";dummy=string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(OBJECT_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style matrix.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @ParameterizedTest(name = "{index} Ensure that parameter of style \"matrix\" start with a ;parameterName=: {0}")
  @ValueSource(strings = {"", ";dummy", ";foo="})
  void testTransformException(String invalidValue) {
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transform(STRING_PARAM, invalidValue));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style matrix.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  @DisplayName("Ensure that values with leading matrix prefix are forwarded to the related transform methods")
  void testTransform() {
    MatrixTransformer spyTransformer = spy(new MatrixTransformer());
    spyTransformer.transform(ARRAY_PARAM, ";dummy=5");
    verify(spyTransformer).transform(ARRAY_PARAM, ";dummy=5");
  }
}
