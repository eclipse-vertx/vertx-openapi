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

package io.vertx.tests.impl;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.impl.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;

@ExtendWith(VertxExtension.class)
class UtilsTest {

  private static Stream<Arguments> testReadYamlOrJson() throws IOException {
    Path petstoreYaml = getRelatedTestResourcePath("io.vertx.tests").resolve("petstore.yaml");
    Path petstoreJson = getRelatedTestResourcePath("io.vertx.tests").resolve("petstore.json");
    JsonObject expectedJson = Buffer.buffer(Files.readAllBytes(petstoreJson)).toJsonObject();
    return Stream.of(
      Arguments.of(petstoreYaml.toString(), expectedJson),
      Arguments.of(petstoreJson.toString(), expectedJson)
    );
  }

  @ParameterizedTest(name = "{index} test testReadYamlOrJson with: {0}")
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @MethodSource
  void testReadYamlOrJson(String path, JsonObject expected, Vertx vertx, VertxTestContext testContext) {
    Utils.readYamlOrJson(vertx, path).onComplete(testContext.succeeding(json -> testContext.verify(() -> {
      assertThat(json).isEqualTo(expected);
      testContext.completeNow();
    })));
  }

  @Test
  public void testNumericYamlKeysAsString(Vertx vertx, VertxTestContext testContext) {
    Utils.readYamlOrJson(vertx, "quirks/test.yaml")
      .onSuccess(json -> testContext.verify(() -> {
        assertThat(json).isNotNull();
        for (Object key : json.getMap().keySet()) {
          assertThat(key).isInstanceOf(String.class);
        }
        testContext.completeNow();
      }))
      .onFailure(testContext::failNow);
  }

}
