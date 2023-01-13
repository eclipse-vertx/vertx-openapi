package io.vertx.openapi.validation.transformer;

import io.vertx.openapi.contract.Parameter;

/**
 * <p>
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | style  | explode | empty  | string      | array                               | object                   |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | simple | false   | n/a    | blue        | blue,black,brown                    | R,100,G,200,B,150        |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | simple | true    | n/a    | blue        | blue,black,brown                    | R=100,G=200,B=150        |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 */
public class SimpleTransformer extends ParameterTransformer {

  @Override
  protected String[] getArrayValues(Parameter parameter, String rawValue) {
    return rawValue.split(",");
  }

  @Override
  protected String[] getObjectKeysAndValues(Parameter parameter, String rawValue) {
    return parameter.isExplode() ? rawValue.split(",|=") : rawValue.split(",");
  }
}
