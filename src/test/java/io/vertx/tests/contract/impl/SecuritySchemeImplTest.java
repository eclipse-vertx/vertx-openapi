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

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.contract.impl.SecuritySchemeImpl;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(VertxExtension.class)
class SecuritySchemeImplTest {
  private static final Path RESOURCE_PATH = getRelatedTestResourcePath(SecuritySchemeImplTest.class);
  private static final Path VALID_SEC_REQ_JSON = RESOURCE_PATH.resolve("sec_scheme_valid.json");
  private static JsonObject validTestData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_SEC_REQ_JSON.toString()).toJsonObject();
  }

  private static SecuritySchemeImpl fromTestData(String id, JsonObject testData) {
    return new SecuritySchemeImpl(testData.getJsonObject(id));
  }

  private static Stream<Arguments> testGetters() {
    return Stream.of(
        Arguments.of("global_api_key", (Consumer<SecuritySchemeImpl>) it -> {
          assertThat(it.getType()).isEqualTo("apiKey");
          assertThat(it.getIn()).isEqualTo("header");
          assertThat(it.getName()).isNotNull();
        }),
        Arguments.of("api_key", (Consumer<SecuritySchemeImpl>) it -> {
          assertThat(it.getType()).isEqualTo("apiKey");
          assertThat(it.getIn()).isEqualTo("header");
          assertThat(it.getName()).isNotNull();
        }),
        Arguments.of("bearerAuth", (Consumer<SecuritySchemeImpl>) it -> {
          assertThat(it.getType()).isEqualTo("http");
          assertThat(it.getScheme()).isEqualTo("bearer");
        }),
        Arguments.of("oauth2", (Consumer<SecuritySchemeImpl>) it -> {
          assertThat(it.getType()).isEqualTo("oauth2");
          assertThat(it.getFlows()).isNotNull();
          assertThat(it.getFlows().getImplicit()).isNotNull();
          assertThat(it.getFlows().getImplicit().getScopes()).isNotNull();
          assertThat(it.getFlows().getImplicit().getScopes().size()).isEqualTo(2);
          assertThat(it.getFlows().getImplicit().getAuthorizationUrl()).isNotNull();
          assertThat(it.getFlows().getImplicit().getTokenUrl()).isNull();

          assertThat(it.getFlows().getAuthorizationCode()).isNotNull();
          assertThat(it.getFlows().getAuthorizationCode().getScopes()).isNotNull();
          assertThat(it.getFlows().getAuthorizationCode().getScopes().size()).isEqualTo(2);
          assertThat(it.getFlows().getAuthorizationCode().getAuthorizationUrl()).isNotNull();
          assertThat(it.getFlows().getAuthorizationCode().getTokenUrl()).isNotNull();

          assertThat(it.getFlows().getPassword()).isNull();
          assertThat(it.getFlows().getClientCredentials()).isNull();
        }));
  }

  @ParameterizedTest(name = "{index} should build SecurityRequirementImpl correct: {0}")
  @MethodSource
  void testGetters(String testId, Consumer<SecuritySchemeImpl> verifier) {
    SecuritySchemeImpl secReq = fromTestData(testId, validTestData);
    verifier.accept(secReq);
  }
}
