package io.vertx.openapi.validation.validator.transformer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.Parameter;

import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

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
public class SimpleTransformer implements ValueTransformer {

  @Override
  public Object transformObject(Parameter parameter, String rawValue) {
    if (rawValue.isEmpty()) {
      return EMPTY_JSON_OBJECT;
    }
    String effectiveRawValue = parameter.isExplode() ? rawValue.replaceAll("=", ",") : rawValue;
    String[] values = effectiveRawValue.split(",");
    if (values.length % 2 != 0) {
      throw createInvalidValueFormat(parameter);
    }
    JsonObject object = new JsonObject();
    for (int i = 0; i < values.length; i = i + 2) {
      object.put(values[i], transformPrimitive(parameter, values[i + 1]));
    }
    return object;
  }

  @Override
  public Object transformArray(Parameter parameter, String rawValue) {
    if (rawValue.isEmpty()) {
      return EMPTY_JSON_ARRAY;
    }
    JsonArray array = new JsonArray();
    for (String value : rawValue.split(",")) {
      array.add(transformPrimitive(parameter, value));
    }
    return array;
  }
}
