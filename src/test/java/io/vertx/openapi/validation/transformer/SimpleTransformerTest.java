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
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleTransformerTest implements SchemaSupport {

  private static final Parameter OBJECT_PARAM = mockSimpleParameter(OBJECT_SCHEMA, false);
  private static final Parameter ARRAY_PARAM = mockSimpleParameter(ARRAY_SCHEMA, false);
  private static final Parameter STRING_PARAM = mockSimpleParameter(STRING_SCHEMA, false);
  private static final Parameter NUMBER_PARAM = mockSimpleParameter(NUMBER_SCHEMA, false);
  private static final Parameter INTEGER_PARAM = mockSimpleParameter(INTEGER_SCHEMA, false);
  private static final Parameter BOOLEAN_PARAM = mockSimpleParameter(BOOLEAN_SCHEMA, false);

  private static final Parameter OBJECT_PARAM_EXPLODE = mockSimpleParameter(OBJECT_SCHEMA, true);

  private static final SimpleTransformer TRANSFORMER = new SimpleTransformer();

  private static Parameter mockSimpleParameter(JsonSchema schema, boolean explode) {
    return mockParameter(NAME, PATH, SIMPLE, explode, schema);
  }

  private static Stream<Arguments> provideValidPrimitiveValues() {
    return Stream.of(
      Arguments.of("(String) empty", STRING_PARAM, "", ""),
      Arguments.of("(String) 44", STRING_PARAM, "44", "44"),
      Arguments.of("(String) \"foobar\"", STRING_PARAM, "foobar", "foobar"),
      Arguments.of("(Number) 14.6767", NUMBER_PARAM, "14.6767", 14.6767),
      Arguments.of("(Integer) 42", INTEGER_PARAM, "42", 42),
      Arguments.of("(Boolean) true", BOOLEAN_PARAM, "true", true)
    );
  }

  private static Stream<Arguments> provideValidArrayValues() {
    JsonArray expectedComplex = new JsonArray().add("Hello").add(13.37).add(1).add(false);
    return Stream.of(
      Arguments.of("empty", ARRAY_PARAM, "", EMPTY_JSON_ARRAY),
      Arguments.of("3", ARRAY_PARAM, "3", new JsonArray().add(3)),
      Arguments.of("Hello,13.37,1,false", ARRAY_PARAM, "Hello,13.37,1,false", expectedComplex)
    );
  }

  private static Stream<Arguments> provideValidObjectValues() {
    String complexExplodedRaw = "string=foo,number=13.37,integer=42,boolean=true";
    String complexRaw = complexExplodedRaw.replace("=", ",");
    JsonObject expectedComplex =
      new JsonObject().put("string", "foo").put("number", 13.37).put("integer", 42).put("boolean", true);

    return Stream.of(
      Arguments.of("empty", OBJECT_PARAM, "", EMPTY_JSON_OBJECT),
      Arguments.of("empty (exploded)", OBJECT_PARAM_EXPLODE, "", EMPTY_JSON_OBJECT),
      Arguments.of(complexRaw, OBJECT_PARAM, complexRaw, expectedComplex),
      Arguments.of(complexExplodedRaw + " (exploded)", OBJECT_PARAM_EXPLODE, complexExplodedRaw,
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
    assertThrows(DecodeException.class, () -> TRANSFORMER.transformPrimitive(STRING_PARAM, "\""));

    String invalidObject = "string,foo,number";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transformObject(OBJECT_PARAM, invalidObject));
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style simple.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
