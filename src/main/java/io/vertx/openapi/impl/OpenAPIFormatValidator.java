package io.vertx.openapi.impl;

import io.vertx.json.schema.JsonFormatValidator;

public class OpenAPIFormatValidator implements JsonFormatValidator {

  // The formats "float" and "double" define the precision of a number, not its representation. Whole numbers
  // are valid, as long as they can be represented exactly with the related precision.
  private static final long MAX_EXACT_FLOAT_INTEGER = 1L << 24;
  private static final long MAX_EXACT_DOUBLE_INTEGER = 1L << 53;

  @Override
  public String validateFormat(String instanceType, String format, Object instance) {
    if ("int32".equalsIgnoreCase(format) && !(instance instanceof Integer)) {
      return getMessage(format);
    }

    if ("int64".equalsIgnoreCase(format) && !(instance instanceof Integer || instance instanceof Long)) {
      return getMessage(format);
    }

    if ("float".equalsIgnoreCase(format)) {
      if (instance instanceof Integer || instance instanceof Long) {
        if (!isExactlyRepresentable(((Number) instance).longValue(), MAX_EXACT_FLOAT_INTEGER)) {
          return getMessage(format);
        }
        // Behind the scenes we use jackson, so even floats are converted into doubles for us.
        // So now we will down cast the float back into a double, and check the usual isInfinite and isNan.
      } else if (!(instance instanceof Double) || ((Float) ((Double) instance).floatValue()).isInfinite()
          || ((Float) ((Double) instance).floatValue()).isNaN()) {
        return getMessage(format);
      }
    }

    if ("double".equalsIgnoreCase(format)) {
      if (instance instanceof Integer || instance instanceof Long) {
        if (!isExactlyRepresentable(((Number) instance).longValue(), MAX_EXACT_DOUBLE_INTEGER)) {
          return getMessage(format);
        }
      } else if (!(instance instanceof Double) || ((Double) instance).isInfinite() || ((Double) instance).isNaN()) {
        return getMessage(format);
      }
    }

    return null;
  }

  private static boolean isExactlyRepresentable(long value, long maxExactInteger) {
    return value >= -maxExactInteger && value <= maxExactInteger;
  }

  private String getMessage(String format) {
    String type = "int32".equalsIgnoreCase(format) || "int64".equalsIgnoreCase(format) ? "Integer" : "Number";
    return String.format("%s does not match the format \"%s\"", type, format);
  }

}
