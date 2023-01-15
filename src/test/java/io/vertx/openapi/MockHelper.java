package io.vertx.openapi;

import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.common.dsl.SchemaType;
import io.vertx.openapi.contract.Location;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Style;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MockHelper {

  private MockHelper() {

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
