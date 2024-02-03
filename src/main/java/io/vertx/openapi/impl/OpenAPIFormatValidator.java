package io.vertx.openapi.impl;

import io.vertx.json.schema.impl.JsonFormatValidator;

public class OpenAPIFormatValidator extends JsonFormatValidator {
  @Override
  public String validateFormat(String format, Object instance) {

    if ("int32".equalsIgnoreCase(format) && !(instance instanceof Integer)) {
      return getMessage(format);
    }

    if ("int64".equalsIgnoreCase(format) && !(instance instanceof Integer || instance instanceof Long)) {
      return getMessage(format);
    }

    if ("float".equalsIgnoreCase(format) || "double".equalsIgnoreCase(format)) {
      //Behind the scenes we use jackson, so even floats are converted into doubles for us.
      if (!(instance instanceof Double) || ((Double)instance).isInfinite() || ((Double)instance).isNaN()) {
        return getMessage(format);
      }
    }

    return null;
  }

  private String getMessage(String format) {
    String type = "int32".equalsIgnoreCase(format) || "int64".equalsIgnoreCase(format) ? "Integer" : "Number";
    return String.format("%s does not match the format \"%s\"", type, format);
  }

  private boolean isNumeric(Object instance) {
    return instance instanceof Integer || instance instanceof Long || instance instanceof Float || instance instanceof Double;
  }

}
