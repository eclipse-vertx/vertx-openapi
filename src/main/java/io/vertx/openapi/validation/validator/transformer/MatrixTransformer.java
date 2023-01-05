package io.vertx.openapi.validation.validator.transformer;

import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

/**
 * <p>
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | style  | explode | empty  | string      | array                               | object                   |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | matrix | false   | ;color | ;color=blue | ;color=blue,black,brown             | ;color=R,100,G,200,B,150 |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | matrix | true    | ;color | ;color=blue | ;color=blue;color=black;color=brown | ;R=100;G=200;B=150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 */
public class MatrixTransformer extends SimpleTransformer {

  private static String buildPrefix(Parameter parameter) {
    return ";" + parameter.getName() + "=";
  }

  private static boolean hasPrefix(String prefix, String value) {
    return value.startsWith(prefix);
  }

  static String removePrefix(Parameter parameter, String value) {
    String prefix = buildPrefix(parameter);
    return hasPrefix(prefix, value) ? value.substring(prefix.length()) : value;
  }

  @Override
  public Object transform(Parameter parameter, String rawValue) {
    if (!rawValue.isEmpty() && hasPrefix(buildPrefix(parameter), rawValue)) {
      return super.transform(parameter, rawValue);
    } else {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  public Object transformPrimitive(Parameter parameter, String rawValue) {
    return super.transformPrimitive(parameter, removePrefix(parameter, rawValue));
  }

  @Override
  public Object transformObject(Parameter parameter, String rawValue) {
    try {
      if (parameter.isExplode()) {
        String convertedValue = rawValue.replaceAll(";", ",").substring(1);
        return super.transformObject(parameter, convertedValue);
      }
      return super.transformObject(parameter, removePrefix(parameter, rawValue));
    } catch (ValidatorException e) {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  public Object transformArray(Parameter parameter, String rawValue) {
    if (parameter.isExplode()) {
      String convertedValue = rawValue.replaceAll(buildPrefix(parameter), ",").substring(1);
      return super.transformArray(parameter, convertedValue);
    }
    return super.transformArray(parameter, removePrefix(parameter, rawValue));
  }
}
