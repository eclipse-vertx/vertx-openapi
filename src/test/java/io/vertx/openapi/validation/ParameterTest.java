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

package io.vertx.openapi.validation;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;

class ParameterTest {

  private static final JsonArray DEFAULT_JSON_ARRAY = new JsonArray(Arrays.asList("a", "b", "c"));
  public static final JsonObject DEFAULT_JSON_OBJECT = new JsonObject().put("abc", "123");

  private static Stream<Arguments> provideValueTypes() {
    Boolean[] isNumber = new Boolean[] {false, true, false, false, false, false, false, false};
    return Stream.of(
      Arguments.of("String", "myString", new Boolean[] {true, false, false, false, false, false, false, false}),
      Arguments.of("String (empty)", "", new Boolean[] {true, false, false, false, false, false, false, true}),
      Arguments.of("Integer", 1337, isNumber),
      Arguments.of("Long", 42L, isNumber),
      Arguments.of("Float", 13.37f, isNumber),
      Arguments.of("Double", 4.2, isNumber),
      Arguments.of("Boolean", true, new Boolean[] {false, false, true, false, false, false, false, false}),
      Arguments.of("JsonObject (empty)", EMPTY_JSON_OBJECT,
        new Boolean[] {false, false, false, true, false, false, false, true}),
      Arguments.of("JsonObject", new JsonObject().put("key", "value"),
        new Boolean[] {false, false, false, true, false, false, false, false}),
      Arguments.of("JsonArray (empty)", EMPTY_JSON_ARRAY,
        new Boolean[] {false, false, false, false, true, false, false, true}),
      Arguments.of("JsonArray", new JsonArray().add(1),
        new Boolean[] {false, false, false, false, true, false, false, false}),
      Arguments.of("Buffer (empty)", Buffer.buffer(),
        new Boolean[] {false, false, false, false, false, true, false, true}),
      Arguments.of("Buffer", Buffer.buffer("buf"),
        new Boolean[] {false, false, false, false, false, true, false, false}),
      Arguments.of("Null", null, new Boolean[] {false, false, false, false, false, false, true, true})
    );
  }

  private static Stream<Arguments> provideValues() {
    Object[] stringResult = new Object[] {"myString", null, null, null, null, null, null, null, null};
    Object[] booleanResult = new Object[] {null, true, null, null, null, null, null, null, null};
    Object[] jsonObjectResult = new Object[] {null, null, EMPTY_JSON_OBJECT, null, null, null, null, null, null};
    Object[] jsonArrayResult = new Object[] {null, null, null, EMPTY_JSON_ARRAY, null, null, null, null, null};
    Object[] bufferResult = new Object[] {null, null, null, null, Buffer.buffer(), null, null, null, null};
    Object[] intResult = new Object[] {null, null, null, null, null, 1337, 1337L, 1337.0f, 1337.0,};
    Object[] longResult = new Object[] {null, null, null, null, null, 42, 42L, 42.0f, 42.0};
    Object[] floatResult = new Object[] {null, null, null, null, null, 13, 13, 13.37f, 13.37};
    Object[] doubleResult = new Object[] {null, null, null, null, null, 4, 4L, 4.2f, 4.2};

    return Stream.of(
      Arguments.of("String", "myString", stringResult),
      Arguments.of("Boolean", true, booleanResult),
      Arguments.of("JsonObject", EMPTY_JSON_OBJECT, jsonObjectResult),
      Arguments.of("JsonArray", EMPTY_JSON_ARRAY, jsonArrayResult),
      Arguments.of("Buffer (empty)", Buffer.buffer(), bufferResult),
      Arguments.of("Integer", 1337, intResult),
      Arguments.of("Long", 42L, longResult),
      Arguments.of("Float", 13.37f, floatResult),
      Arguments.of("Double", 4.2, doubleResult)
    );
  }

  private static Stream<Arguments> provideAllNullValues() {
    Object[] stringResult = new Object[] {"myString", false, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 8008, 8008L, 8008.0f, 8008.0};
    Object[] booleanResult = new Object[] {"myDefaultString", true, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 8008, 8008L, 8008.0f, 8008.0};
    Object[] jsonObjectResult = new Object[] {"myDefaultString", false, EMPTY_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 8008, 8008L, 8008.0f, 8008.0};
    Object[] jsonArrayResult = new Object[] {"myDefaultString", false, DEFAULT_JSON_OBJECT, EMPTY_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 8008, 8008L, 8008.0f, 8008.0};
    Object[] bufferResult = new Object[] {"myDefaultString", false, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, Buffer.buffer(), 8008, 8008L, 8008.0f, 8008.0};
    Object[] intResult = new Object[] {"myDefaultString", false, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 1337, 1337L, 1337.0f, 1337.0,};
    Object[] longResult = new Object[] {"myDefaultString", false, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 42, 42L, 42.0f, 42.0};
    Object[] floatResult = new Object[] {"myDefaultString", false, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 13, 13, 13.37f, 13.37};
    Object[] doubleResult = new Object[] {"myDefaultString", false, DEFAULT_JSON_OBJECT, DEFAULT_JSON_ARRAY, DEFAULT_JSON_OBJECT.toBuffer(), 4, 4L, 4.2f, 4.2};

    return Stream.of(
      Arguments.of("String", "myString", stringResult),
      Arguments.of("Boolean", true, booleanResult),
      Arguments.of("JsonObject", EMPTY_JSON_OBJECT, jsonObjectResult),
      Arguments.of("JsonArray", EMPTY_JSON_ARRAY, jsonArrayResult),
      Arguments.of("Buffer (empty)", Buffer.buffer(), bufferResult),
      Arguments.of("Integer", 1337, intResult),
      Arguments.of("Long", 42L, longResult),
      Arguments.of("Float", 13.37f, floatResult),
      Arguments.of("Double", 4.2, doubleResult)
    );
  }

  @ParameterizedTest(name = "{index} test if value is of type {0}")
  @MethodSource("provideValueTypes")
  void testIsMethods(String type, Object value, Boolean[] expected) {
    DummyParameter parameter = new DummyParameter().setValue(value);

    Boolean[] results = new Boolean[8];
    results[0] = parameter.isString();
    results[1] = parameter.isNumber();
    results[2] = parameter.isBoolean();
    results[3] = parameter.isJsonObject();
    results[4] = parameter.isJsonArray();
    results[5] = parameter.isBuffer();
    results[6] = parameter.isNull();
    results[7] = parameter.isEmpty();

    assertThat(results).asList().containsExactlyElementsIn(expected).inOrder();
  }

  @ParameterizedTest(name = "{index} test getters with value of type {0}")
  @MethodSource("provideValues")
  void testGetters(String type, Object value, Object[] expected) {
    DummyParameter parameter = new DummyParameter().setValue(value);

    Object[] results = new Object[9];
    results[0] = parameter.getString();
    results[1] = parameter.getBoolean();
    results[2] = parameter.getJsonObject();
    results[3] = parameter.getJsonArray();
    results[4] = parameter.getBuffer();
    results[5] = parameter.getInteger();
    results[6] = parameter.getLong();
    results[7] = parameter.getFloat();
    results[8] = parameter.getDouble();

    for (int i = 0; i < 8; i++) {
      assertThat(results[i]).isEqualTo(expected[i]);
    }
    if (expected[8] != null) {
      Double expectedDouble = (Double) expected[8];
      assertThat((Double) results[8]).isWithin(0.1).of(expectedDouble);
    }
  }

  @ParameterizedTest(name = "{index} test default getters with value of type {0}")
  @MethodSource("provideAllNullValues")
  void testDefaultGetters(String type, Object value, Object[] expected) {
    DummyParameter parameter = new DummyParameter().setValue(value);

    Object[] results = new Object[9];
    results[0] = parameter.getString(() -> "myDefaultString");
    results[1] = parameter.getBoolean(() -> false);
    results[2] = parameter.getJsonObject(() -> DEFAULT_JSON_OBJECT);
    results[3] = parameter.getJsonArray(() -> DEFAULT_JSON_ARRAY);
    results[4] = parameter.getBuffer(DEFAULT_JSON_OBJECT::toBuffer);
    results[5] = parameter.getInteger(() -> 8008);
    results[6] = parameter.getLong(() -> 8008L);
    results[7] = parameter.getFloatOrDefault(() -> 8008.0f);
    results[8] = parameter.getDouble(() -> 8008.0);

    for (int i = 0; i < 8; i++) {
      assertThat(results[i]).isEqualTo(expected[i]);
    }
    if (expected[8] != null) {
      Double expectedDouble = (Double) expected[8];
      assertThat((Double) results[8]).isWithin(0.1).of(expectedDouble);
    }
  }

  static class DummyParameter implements Parameter {
    private Object value;

    DummyParameter setValue(Object value) {
      this.value = value;
      return this;
    }

    @Override
    public Object get() {
      return value;
    }
  }

}
