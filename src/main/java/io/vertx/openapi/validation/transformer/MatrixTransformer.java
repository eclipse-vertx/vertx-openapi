package io.vertx.openapi.validation.transformer;

import io.vertx.openapi.contract.Parameter;

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
public class MatrixTransformer extends ParameterTransformer {

  String buildPrefix(Parameter parameter) {
    if (parameter.isExplode() && "object".equals(getSchemaType(parameter))) {
      return ";";
    }
    return ";" + parameter.getName() + "=";
  }

  @Override
  public Object transform(Parameter parameter, String rawValue) {
    String prefix = buildPrefix(parameter);
    if (!rawValue.isEmpty() && rawValue.startsWith(prefix)) {
      return super.transform(parameter, rawValue.substring(prefix.length()));
    } else {
      throw createInvalidValueFormat(parameter);
    }
  }

  @Override
  protected String[] getArrayValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split(buildPrefix(parameter)) : rawValue.split(",");

  }

  @Override protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split("(" + buildPrefix(parameter) + "|=)") : rawValue.split(",");
  }
}
