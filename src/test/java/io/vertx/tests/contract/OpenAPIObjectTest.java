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

package io.vertx.tests.contract;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.OpenAPIObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static io.vertx.tests.ResourceHelper.loadJson;
import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(VertxExtension.class)
class OpenAPIObjectTest {
  private static final Path MODELS_WITH_EXTENSIONS = getRelatedTestResourcePath(OpenAPIObjectTest.class).resolve(
    "models_with_extensions.json");
  private static JsonObject testData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx) {
    testData = loadJson(vertx, MODELS_WITH_EXTENSIONS);
  }

  private static Stream<Arguments> provideScenarios() {
    Map<String, Object> oneString = ImmutableMap.of("x-some-string", "someString");
    Map<String, Object> oneStringOneArray = ImmutableMap.<String, Object>builder().putAll(oneString)
      .put("x-some-array", new JsonArray().add("foo").add("bar")).build();
    Map<String, Object> oneStringOneArrayOneNumber = ImmutableMap.<String, Object>builder().putAll(oneStringOneArray)
      .put("x-some-number", 1337).build();

    return Stream.of(
      Arguments.of("0000_Operation_With_One_String", oneString),
      Arguments.of("0001_Operation_With_One_String_One_Array", oneStringOneArray),
      Arguments.of("0002_Operation_With_One_String_One_Array_One_Number", oneStringOneArrayOneNumber)
    );
  }

  @ParameterizedTest(name = "{index} should throw an exception for scenario: {0}")
  @MethodSource(value = "provideScenarios")
  void testGetExtensions(String testId, Map<String, Object> expected) {
    OpenAPIObject object = () -> testData.getJsonObject(testId);
    assertThat(object.getExtensions()).containsExactlyEntriesIn(expected);
  }
}
