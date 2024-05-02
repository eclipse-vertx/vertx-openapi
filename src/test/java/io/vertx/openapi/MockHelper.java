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

package io.vertx.openapi;

import io.vertx.core.buffer.Buffer;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.validation.ValidatableRequest;
import io.vertx.openapi.validation.impl.RequestParameterImpl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MockHelper {

  private MockHelper() {

  }

  public static ValidatableRequest mockValidatableRequest(Buffer body, String contentType) {
    ValidatableRequest mockedRequest = mock(ValidatableRequest.class);
    when(mockedRequest.getBody()).thenReturn(new RequestParameterImpl(body));
    when(mockedRequest.getContentType()).thenReturn(contentType);
    return mockedRequest;
  }

  public static Parameter mockParameter(String name, Location in, Style style, boolean explode, JsonSchema schema) {
    return mockParameter(name, in, style, explode, schema, false);
  }

  public static Parameter mockParameter(String name, Location in, Style style, boolean explode, JsonSchema schema,
                                        boolean required) {
    Parameter mockedParam = mock(Parameter.class);
    when(mockedParam.getName()).thenReturn(name);
    when(mockedParam.getIn()).thenReturn(in);
    when(mockedParam.getStyle()).thenReturn(style);
    when(mockedParam.isExplode()).thenReturn(explode);
    when(mockedParam.getSchema()).thenReturn(schema);
    when(mockedParam.isRequired()).thenReturn(required);
    SchemaType type = SchemaType.valueOf(schema.<String>get("type").toUpperCase());
    when(mockedParam.getSchemaType()).thenReturn(type);

    return mockedParam;
  }

}
