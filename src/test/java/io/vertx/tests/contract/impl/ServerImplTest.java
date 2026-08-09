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
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Server;
import io.vertx.openapi.contract.impl.ServerImpl;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ServerImplTest {

  @Test
  void testGetters() {
    String url = "http://foo.bar/foobar";
    JsonObject model = new JsonObject().put("url", url);
    Server server = new ServerImpl(model);

    assertThat(server.getOpenAPIModel()).isEqualTo(model);
    assertThat(server.getURL()).isEqualTo(url);
    assertThat(server.getBasePath()).isEqualTo("/foobar");
  }

  private static Stream<Arguments> testBasePathExtraction() {
    return Stream.of(
        Arguments.of("https://example.com", ""),
        Arguments.of("https://example.com/", ""),
        Arguments.of("https://example.com/foo", "/foo"),
        Arguments.of("https://example.com/foo/", "/foo"),
        Arguments.of("https://example.com/foo/bar", "/foo/bar"),
        Arguments.of("/", ""),
        Arguments.of("/v1/api", "/v1/api"),
        Arguments.of("/v1/api/", "/v1/api"),
        Arguments.of("//example.com/foo", "/foo"));
  }

  @ParameterizedTest(name = "{index} BasePath extraction: {0} should result into {1}")
  @MethodSource
  void testBasePathExtraction(String url, String basePath) {
    JsonObject model = new JsonObject().put("url", url);
    Server server = new ServerImpl(model);
    assertThat(server.getBasePath()).isEqualTo(basePath);
  }

  @Test
  void testRelativeUrlGetters() {
    String url = "/v1/api";
    JsonObject model = new JsonObject().put("url", url);
    Server server = new ServerImpl(model);

    assertThat(server.getOpenAPIModel()).isEqualTo(model);
    assertThat(server.getURL()).isEqualTo(url);
    assertThat(server.getBasePath()).isEqualTo("/v1/api");
  }

  @Test
  void testExceptions() {
    String msgUnsupported = "The passed OpenAPI contract contains a feature that is not supported: Server Variables";

    OpenAPIContractException exceptionUnsupported =
        assertThrows(OpenAPIContractException.class,
            () -> new ServerImpl(new JsonObject().put("url", "http://{foo}.bar")));
    assertThat(exceptionUnsupported.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionUnsupported).hasMessageThat().isEqualTo(msgUnsupported);

    String msgInvalid = "The passed OpenAPI contract is invalid: The specified URL is malformed: http://foo.bar:-80";
    OpenAPIContractException exceptionInvalid =
        assertThrows(OpenAPIContractException.class,
            () -> new ServerImpl(new JsonObject().put("url", "http://foo.bar:-80")));
    assertThat(exceptionInvalid.type()).isEqualTo(ContractErrorType.INVALID_SPEC);
    assertThat(exceptionInvalid).hasMessageThat().isEqualTo(msgInvalid);

    String msgInvalidRelative = "The passed OpenAPI contract is invalid: The specified URL is malformed: /v1/ api";
    OpenAPIContractException exceptionInvalidRelative =
        assertThrows(OpenAPIContractException.class,
            () -> new ServerImpl(new JsonObject().put("url", "/v1/ api")));
    assertThat(exceptionInvalidRelative.type()).isEqualTo(ContractErrorType.INVALID_SPEC);
    assertThat(exceptionInvalidRelative).hasMessageThat().isEqualTo(msgInvalidRelative);
  }
}
