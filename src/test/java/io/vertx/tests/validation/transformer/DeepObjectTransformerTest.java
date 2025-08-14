/*
 * Copyright (c) 2011-2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.tests.validation.transformer;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.transformer.DeepObjectTransformer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.contract.Location.QUERY;
import static io.vertx.openapi.contract.Style.DEEP_OBJECT;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.tests.MockHelper.mockParameter;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeepObjectTransformerTest implements SchemaSupport {

  private static final Parameter DEEP_OBJECT_PARAM = mockParameter(NAME, QUERY, DEEP_OBJECT, true, OBJECT_SCHEMA);
  private static final Parameter DEEP_OBJECT_ARRAY_PARAM = mockParameter(NAME, QUERY, DEEP_OBJECT, true, ARRAY_SCHEMA);

  private static final DeepObjectTransformer TRANSFORMER = new DeepObjectTransformer();

  private static Stream<Arguments> provideValidObjectValues() {
    String complexExplodedRaw = "dummy[role]=admin&dummy[firstName]=Alex";
    String complexExplodedRawWithExtras = "something=nothing&dummy[role]=admin&dummy[firstName]=Alex";
    String complexExplodedRawWithAnotherParameter = "dummy[role]=admin&dummy[firstName]=Alex&silly[role]=user&silly[firstName]=John";
    JsonObject expected = new JsonObject().put("role", "admin").put("firstName", "Alex");

    return Stream.of(
      Arguments.of("empty", DEEP_OBJECT_PARAM, "", EMPTY_JSON_OBJECT),
      Arguments.of(complexExplodedRaw + " (exploded)", DEEP_OBJECT_PARAM, complexExplodedRaw, expected),
      Arguments.of(complexExplodedRawWithExtras + " (exploded)", DEEP_OBJECT_PARAM, complexExplodedRawWithExtras, expected),
      Arguments.of(complexExplodedRawWithAnotherParameter + " (exploded)", DEEP_OBJECT_PARAM, complexExplodedRawWithAnotherParameter, expected)
    );
  }

  @ParameterizedTest(name = "{index} Transform \"Query\" parameter of style \"deepObject\" with object value: {0}")
  @MethodSource("provideValidObjectValues")
  void testTransformObjectValid(String scenario, Parameter parameter, String rawValue, Object expectedValue) {
    assertThat(TRANSFORMER.transformObject(parameter, rawValue)).isEqualTo(expectedValue);
  }

  @Test
  void testInvalidTransformation() {
    String validExplodedRaw = "dummy[role]=admin&dummy[firstName]=Alex";
    ValidatorException exception =
      assertThrows(ValidatorException.class, () -> TRANSFORMER.transform(DEEP_OBJECT_ARRAY_PARAM, validExplodedRaw));
    String expectedMsg = "Transformation in style deepObject to schema type ARRAY is not supported.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
