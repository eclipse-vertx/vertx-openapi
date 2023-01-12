package io.vertx.openapi.validation.transformer;

import io.vertx.openapi.contract.Parameter;

/**
 * <p>
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 * | style  | explode | empty  | string     | array                              | object                  |
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 * | form   | false   | color= | color=blue | color=blue,black,brown             | color=R,100,G,200,B,150 |
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 * | form   | true    | color= | color=blue | color=blue&color=black&color=brown | R=100&G=200&B=150       |
 * +--------+---------+--------+------------+------------------------------------+-------------------------+
 */
public class FormTransformer extends SimpleTransformer {

  private static String buildPrefix(Parameter parameter) {
    return parameter.getName() + "=";
  }

  @Override
  public Object transformArray(Parameter parameter, String rawValue) {
    if (parameter.isExplode()) {
      String convertedValue = rawValue.replace(buildPrefix(parameter), "").replace("&", ",");
      return super.transformArray(parameter, convertedValue);
    }
    return super.transformArray(parameter, rawValue);
  }

  @Override
  public Object transformObject(Parameter parameter, String rawValue) {
    if (parameter.isExplode()) {
      String convertedValue = rawValue.replace("&", ",");
      return super.transformObject(parameter, convertedValue);
    }
    return super.transformObject(parameter, rawValue);
  }
}
