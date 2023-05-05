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

package io.vertx.openapi.contract.impl;

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContractException;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaTypeImplTest {
  private static final String DUMMY_IDENTIFIER = APPLICATION_JSON.toString();

  @Test
  void testGetters() {
    JsonObject model = new JsonObject().put("schema", stringSchema().toJson());
    MediaType mediaType = new MediaTypeImpl(DUMMY_IDENTIFIER, model);

    assertThat(mediaType.getOpenAPIModel()).isEqualTo(model);
    assertThat(mediaType.getSchema().fieldNames()).containsExactly("type", "$id");
    assertThat(mediaType.getIdentifier()).isEqualTo(DUMMY_IDENTIFIER);
  }

  @Test
  void testExceptions() {
    String msg = "The passed OpenAPI contract contains a feature that is not supported: Media Type without a schema";

    OpenAPIContractException exceptionNull =
      assertThrows(OpenAPIContractException.class, () -> new MediaTypeImpl(DUMMY_IDENTIFIER,
        new JsonObject().putNull("schema")));
    assertThat(exceptionNull.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionNull).hasMessageThat().isEqualTo(msg);

    OpenAPIContractException exceptionEmpty =
      assertThrows(OpenAPIContractException.class,
        () -> new MediaTypeImpl(DUMMY_IDENTIFIER, new JsonObject().put("schema", EMPTY_JSON_OBJECT)));
    assertThat(exceptionEmpty.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionEmpty).hasMessageThat().isEqualTo(msg);
  }
}
