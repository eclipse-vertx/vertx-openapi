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

import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.impl.MediaTypeImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaTypeImplTest {
  private static final String DUMMY_IDENTIFIER = APPLICATION_JSON.toString();
  private static final String DUMMY_REF = "__dummy_unknown_ref__";
  private static final String ABS_URI = "__absolute_uri__";
  private static final String ABS_RECURSIVE_REF = "__absolute_recursive_ref__";
  private static final String ABS_REF = "__absolute_ref__";
  private static final String DUMMY_REF_VALUE = "dummy-ref-value";

  private static Stream<Arguments> testGetters() {
    return Stream.of(
      Arguments.of("MediaType model defined, with no internal annotations", new JsonObject()
        .put("schema", stringSchema().toJson()), List.of("type", "$id")),
      Arguments.of("MediaType model defined, with an unknown internal annotation", new JsonObject()
        .put(DUMMY_REF, DUMMY_REF_VALUE).put("schema", stringSchema().toJson()), List.of("type", "$id")),
      Arguments.of("MediaType model defined, with multiple internal annotations", new JsonObject()
        .put(ABS_URI, DUMMY_REF_VALUE)
        .put(ABS_RECURSIVE_REF, DUMMY_REF_VALUE)
        .put("schema", stringSchema().toJson()), List.of("type", "$id")),
      Arguments.of("No MediaType model defined, with an unknown internal annotation", new JsonObject()
        .put(DUMMY_REF, DUMMY_REF_VALUE), List.of()),
      Arguments.of("No MediaType model defined, with absolute_uri internal annotation",
        new JsonObject().put(ABS_URI, DUMMY_REF_VALUE), List.of()),
      Arguments.of("No MediaType model defined, with absolute_recursive_ref internal annotation",
        new JsonObject().put(ABS_RECURSIVE_REF, DUMMY_REF_VALUE), List.of()),
      Arguments.of("No MediaType model defined, with absolute_ref internal annotation",
        new JsonObject().put(ABS_REF, DUMMY_REF_VALUE), List.of()),
      Arguments.of("No MediaType model defined, with multiple internal annotations", new JsonObject()
        .put(ABS_URI, DUMMY_REF_VALUE)
        .put(ABS_RECURSIVE_REF, DUMMY_REF_VALUE), List.of()),
      Arguments.of("No MediaType model defined, with no internal annotations",
        EMPTY_JSON_OBJECT, List.of())
    );
  }

  @ParameterizedTest(name = "{index} test getters for scenario: {0}")
  @MethodSource
  void testGetters(String scenario, JsonObject mediaTypeModel, List<String> fieldNames) {
    MediaType mediaType = new MediaTypeImpl(DUMMY_IDENTIFIER, mediaTypeModel);
    assertThat(mediaType.getOpenAPIModel()).isEqualTo(mediaTypeModel);
    if (fieldNames.isEmpty()) {
      assertThat(mediaType.getSchema()).isNull();
    } else {
      assertThat(mediaType.getSchema().fieldNames()).containsExactlyElementsIn(fieldNames);
    }
    assertThat(mediaType.getIdentifier()).isEqualTo(DUMMY_IDENTIFIER);
  }

  @Test
  void testExceptions() {
    String msg = "The passed OpenAPI contract contains a feature that is not supported: Media Type without a schema";

    OpenAPIContractException exceptionNoModel =
      assertThrows(OpenAPIContractException.class, () -> new MediaTypeImpl(DUMMY_IDENTIFIER, null));
    assertThat(exceptionNoModel.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionNoModel).hasMessageThat().isEqualTo(msg);

    OpenAPIContractException exceptionSchemaNull =
      assertThrows(OpenAPIContractException.class, () -> new MediaTypeImpl(DUMMY_IDENTIFIER,
        new JsonObject().putNull("schema")));
    assertThat(exceptionSchemaNull.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionSchemaNull).hasMessageThat().isEqualTo(msg);

    OpenAPIContractException exceptionSchemaEmpty =
      assertThrows(OpenAPIContractException.class,
        () -> new MediaTypeImpl(DUMMY_IDENTIFIER, new JsonObject().put("schema", EMPTY_JSON_OBJECT)));
    assertThat(exceptionSchemaEmpty.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionSchemaEmpty).hasMessageThat().isEqualTo(msg);
  }
}
