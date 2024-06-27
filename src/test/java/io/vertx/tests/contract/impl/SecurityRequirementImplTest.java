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

package io.vertx.tests.contract.impl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.impl.SecurityRequirementImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(VertxExtension.class)
class SecurityRequirementImplTest {
  private static final Path RESOURCE_PATH = getRelatedTestResourcePath(SecurityRequirementImplTest.class);
  private static final Path VALID_SEC_REQ_JSON = RESOURCE_PATH.resolve("sec_req_valid.json");
  private static JsonObject validTestData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_SEC_REQ_JSON.toString()).toJsonObject();
  }

  private static SecurityRequirementImpl fromTestData(String id, JsonObject testData) {
    return new SecurityRequirementImpl(testData.getJsonObject(id));
  }

  private static Stream<Arguments> testGetters() {
    return Stream.of(
      Arguments.of("0000_Test_No_Name_No_Scopes_(EMPTY)", (Consumer<SecurityRequirementImpl>) seqReq -> {
        assertThat(seqReq.isEmpty()).isTrue();
        assertThat(seqReq.size()).isEqualTo(0);
        assertThat(seqReq.getNames()).isEmpty();
      }),
      Arguments.of("0001_Test_One_Name_No_Scope", (Consumer<SecurityRequirementImpl>) seqReq -> {
        assertThat(seqReq.isEmpty()).isFalse();
        assertThat(seqReq.size()).isEqualTo(1);
        assertThat(seqReq.getNames()).containsExactly("api_key");
        assertThat(seqReq.getScopes("api_key")).isEmpty();
      }),
      Arguments.of("0002_Test_One_Name_Two_Scopes", (Consumer<SecurityRequirementImpl>) seqReq -> {
        assertThat(seqReq.isEmpty()).isFalse();
        assertThat(seqReq.size()).isEqualTo(1);
        assertThat(seqReq.getNames()).containsExactly("api_key");
        assertThat(seqReq.getScopes("api_key")).containsExactly("scope1", "scope2").inOrder();
      }),
      Arguments.of("0003_Test_Two_Names_No_Scopes", (Consumer<SecurityRequirementImpl>) seqReq -> {
        assertThat(seqReq.isEmpty()).isFalse();
        assertThat(seqReq.size()).isEqualTo(2);
        assertThat(seqReq.getNames()).containsExactly("api_key", "second_api_key");
        assertThat(seqReq.getScopes("api_key")).isEmpty();
        assertThat(seqReq.getScopes("second_api_key")).isEmpty();
      }),
      Arguments.of("0004_Test_Two_Names_One_Scope_Each", (Consumer<SecurityRequirementImpl>) seqReq -> {
        assertThat(seqReq.isEmpty()).isFalse();
        assertThat(seqReq.size()).isEqualTo(2);
        assertThat(seqReq.getNames()).containsExactly("api_key", "second_api_key");
        assertThat(seqReq.getScopes("api_key")).containsExactly("scope1");
        assertThat(seqReq.getScopes("second_api_key")).containsExactly("scope2");
      })
    );
  }

  @ParameterizedTest(name = "{index} should build SecurityRequirementImpl correct: {0}")
  @MethodSource
  void testGetters(String testId, Consumer<SecurityRequirementImpl> verifier) {
    SecurityRequirementImpl secReq = fromTestData(testId, validTestData);
    verifier.accept(secReq);
  }

  @Test
  void testGetScopesError() {
    SecurityRequirementImpl secReq = fromTestData("0001_Test_One_Name_No_Scope", validTestData);
    IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
      () -> secReq.getScopes("Hodor"));
    assertThat(exception).hasMessageThat().isEqualTo("No security requirement with name Hodor");
  }
}
