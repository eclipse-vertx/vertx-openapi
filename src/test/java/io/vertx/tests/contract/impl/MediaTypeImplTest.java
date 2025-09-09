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
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.ContractErrorType;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.impl.MediaTypeImpl;
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.analyser.ContentAnalyser;
import io.vertx.openapi.validation.analyser.ContentAnalyserFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MediaTypeImplTest {
  private static final String DUMMY_IDENTIFIER = APPLICATION_JSON.toString();
  private static final String DUMMY_REF = "__dummy_unknown_ref__";
  private static final String ABS_URI = "__absolute_uri__";
  private static final String ABS_RECURSIVE_REF = "__absolute_recursive_ref__";
  private static final String ABS_REF = "__absolute_ref__";
  private static final String DUMMY_REF_VALUE = "dummy-ref-value";

  private static Stream<Arguments> testGetters() {
    var partialSchemaJson = JsonObject.of("schema", stringSchema().toJson());

    var partialUnknownAnnotation = JsonObject.of(DUMMY_REF, DUMMY_REF_VALUE);
    var partialAbsoluteUri = JsonObject.of(ABS_URI, DUMMY_REF_VALUE);
    var partialAbsoluteRecursiveRef = JsonObject.of(ABS_RECURSIVE_REF, DUMMY_REF_VALUE);
    var partialAbsoluteRef = JsonObject.of(ABS_REF, DUMMY_REF_VALUE);
    var partialMultipleAnnotations = partialAbsoluteUri.copy().mergeIn(partialUnknownAnnotation);

    return Stream.of(
        Arguments.of("MediaType model defined, with no internal annotations", partialSchemaJson,
            List.of("type", "$id")),
        Arguments.of("MediaType model defined, with an unknown internal annotation", partialSchemaJson
            .copy().mergeIn(partialUnknownAnnotation), List.of("type", "$id")),
        Arguments.of("MediaType model defined, with multiple internal annotations", partialSchemaJson
            .copy().mergeIn(partialMultipleAnnotations), List.of("type", "$id")),
        Arguments.of("No MediaType model defined, with an unknown internal annotation",
            partialUnknownAnnotation, List.of()),
        Arguments.of("No MediaType model defined, with absolute_uri internal annotation",
            partialAbsoluteUri, List.of()),
        Arguments.of("No MediaType model defined, with absolute_recursive_ref internal annotation",
            partialAbsoluteRecursiveRef, List.of()),
        Arguments.of("No MediaType model defined, with absolute_ref internal annotation",
            partialAbsoluteRef, List.of()),
        Arguments.of("No MediaType model defined, with multiple internal annotations",
            partialMultipleAnnotations, List.of()),
        Arguments.of("No MediaType model defined, with no internal annotations",
            EMPTY_JSON_OBJECT, List.of()));
  }

  @ParameterizedTest(name = "{index} test getters for scenario: {0}")
  @MethodSource
  void testGetters(String scenario, JsonObject mediaTypeModel, List<String> fieldNames) {
    MediaType mediaType = new MediaTypeImpl(DUMMY_IDENTIFIER, mediaTypeModel, emptyMap());
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
        assertThrows(OpenAPIContractException.class, () -> new MediaTypeImpl(DUMMY_IDENTIFIER, null, emptyMap()));
    assertThat(exceptionNoModel.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionNoModel).hasMessageThat().isEqualTo(msg);

    OpenAPIContractException exceptionSchemaNull =
        assertThrows(OpenAPIContractException.class, () -> new MediaTypeImpl(DUMMY_IDENTIFIER,
            new JsonObject().putNull("schema"), emptyMap()));
    assertThat(exceptionSchemaNull.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionSchemaNull).hasMessageThat().isEqualTo(msg);

    OpenAPIContractException exceptionSchemaEmpty =
        assertThrows(OpenAPIContractException.class,
            () -> new MediaTypeImpl(DUMMY_IDENTIFIER, new JsonObject().put("schema", EMPTY_JSON_OBJECT), emptyMap()));
    assertThat(exceptionSchemaEmpty.type()).isEqualTo(ContractErrorType.UNSUPPORTED_FEATURE);
    assertThat(exceptionSchemaEmpty).hasMessageThat().isEqualTo(msg);
  }

  @Test
  void testCustomMediaTypes() {
    var mt1 = new MediaTypeImpl(
        "text/event-stream",
        JsonObject.of("schema", stringSchema().toJson()),
        Map.of("text/event-stream", DummyContentAnalyzer.FACTORY, "application/xml", ContentAnalyserFactory.NO_OP)
    );

    ContentAnalyserFactory factory = mt1.getContentAnalyserFactory();
    assertNotNull(factory);
    ContentAnalyser analyser = factory.getContentAnalyser("text/event-stream", Buffer.buffer("Hello world!"), null);
    assertInstanceOf(DummyContentAnalyzer.class, analyser);
    assertEquals(Buffer.buffer("Hello world!"), analyser.transform());

    var mt2 = new MediaTypeImpl(
        "application/xml",
        JsonObject.of("schema", stringSchema().toJson()),
        Map.of("text/event-stream", ContentAnalyserFactory.NO_OP)
    );

    assertNull(mt2.getContentAnalyserFactory());
  }

  public static class DummyContentAnalyzer extends ContentAnalyser {
    public static final ContentAnalyserFactory FACTORY = DummyContentAnalyzer::new;

    /**
     * Creates a new content analyser.
     *
     * @param contentType the content type.
     * @param content     the content to be analysed.
     * @param context     the context in which the content is used.
     */
    public DummyContentAnalyzer(String contentType, Buffer content, ValidationContext context) {
      super(contentType, content, context);
    }

    @Override
    public void checkSyntacticalCorrectness() {
      // For testing purposes, this is a no-op
    }

    @Override
    public Object transform() {
      return content;
    }
  }
}
