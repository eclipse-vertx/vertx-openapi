package io.vertx.openapi.validation.validator.transformer;

import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

/**
 * <p>
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | style  | explode | empty  | string      | array                               | object                   |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | label  | false   | .      | .blue       | .blue.black.brown                   | .R.100.G.200.B.150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | label  | true    | .      | .blue       | .blue.black.brown                   | .R=100.G=200.B=150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 */
public class LabelTransformer extends SimpleTransformer {

  private static boolean startsWithDot(String value) {
    return '.' == value.charAt(0);
  }

  /**
   * @return a String without the preceded dot.
   */
  private static String removePrecededDot(String value) {
    return startsWithDot(value) ? value.substring(1) : value;
  }

  @Override
  public Object transform(Parameter parameter, String rawValue) {
    if (!rawValue.isEmpty() && startsWithDot(rawValue)) {
      return super.transform(parameter, rawValue);
    } else {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  public Object transformPrimitive(Parameter parameter, String rawValue) {
    return super.transformPrimitive(parameter, removePrecededDot(rawValue));
  }

  @Override
  public Object transformObject(Parameter parameter, String rawValue) {
    try {
      if (parameter.isExplode()) {
        return super.transformObject(parameter, removePrecededDot(rawValue).replaceAll("\\.", ","));
      }
      return super.transformObject(parameter, removePrecededDot(rawValue));
    } catch (ValidatorException e) {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  public Object transformArray(Parameter parameter, String rawValue) {
    if (parameter.isExplode()) {
      return super.transformArray(parameter, (removePrecededDot(rawValue).replaceAll("\\.", ",")));
    }
    return super.transformArray(parameter, removePrecededDot(rawValue));
  }
}
