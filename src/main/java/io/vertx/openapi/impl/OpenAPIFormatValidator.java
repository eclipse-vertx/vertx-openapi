package io.vertx.openapi.impl;

import io.vertx.json.schema.JsonFormatValidator;

public class OpenAPIFormatValidator implements JsonFormatValidator {

  @Override
  public String validateFormat(String instanceType, String format, Object instance) {
    if ("int32".equalsIgnoreCase(format) && !(instance instanceof Integer)) {
      return getMessage(format);
    }

    if ("int64".equalsIgnoreCase(format) && !(instance instanceof Integer || instance instanceof Long)) {
      return getMessage(format);
    }

    if ("float".equalsIgnoreCase(format)) {
      // Behind the scenes we use jackson, so even floats are converted into doubles for us.
      // So now we will down cast the float back into a double, and check the usual isInfinite and isNan.
      if (!(instance instanceof Double) || ((Float) ((Double) instance).floatValue()).isInfinite()
          || ((Float) ((Double) instance).floatValue()).isNaN()) {
        return getMessage(format);
      }
    }

    if ("float".equalsIgnoreCase(format) || "double".equalsIgnoreCase(format)) {
      if (!(instance instanceof Double) || ((Double) instance).isInfinite() || ((Double) instance).isNaN()) {
        return getMessage(format);
      }
    }

    return null;
  }

  private String getMessage(String format) {
    String type = "int32".equalsIgnoreCase(format) || "int64".equalsIgnoreCase(format) ? "Integer" : "Number";
    return String.format("%s does not match the format \"%s\"", type, format);
  }

}
