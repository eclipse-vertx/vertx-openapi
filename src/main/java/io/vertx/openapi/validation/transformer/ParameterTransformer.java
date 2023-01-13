package io.vertx.openapi.validation.transformer;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.Parameter;

import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.validation.ValidatorException.createCantDecodeValue;
import static io.vertx.openapi.validation.ValidatorException.createInvalidValueFormat;

public abstract class ParameterTransformer {

  /**
   * Returns the schema type (string, integer, number, boolean, array, object) of the passed {@link Parameter}.
   *
   * @param parameter The related {@link Parameter}.
   * @return the schema type (string, integer, number, boolean, array, object).
   */
  protected String getSchemaType(Parameter parameter) {
    return parameter.getSchema().<String>get("type");
  }

  /**
   * Transforms the raw value from its {@link String} representation into JSON. This method does not only decode a
   * {@link String}, it also takes the different {@link io.vertx.openapi.contract.Style styles} into account}.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transform(Parameter parameter, String rawValue) {
    try {
      switch (getSchemaType(parameter)) {
        case "object":
          return transformObject(parameter, rawValue);
        case "array":
          return transformArray(parameter, rawValue);
        default:
          return transformPrimitive(parameter, rawValue);
      }
    } catch (DecodeException e) {
      throw createCantDecodeValue(parameter);
    }
  }

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be primitive.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transformPrimitive(Parameter parameter, String rawValue) {
    try {
      return Json.decodeValue(rawValue);
    } catch (DecodeException de) {
      if (rawValue.isEmpty()) {
        return rawValue;
      } else if ('"' == rawValue.charAt(0)) {
        throw de;
      } else {
        // let's try it as JSON String
        String stringified = "\"" + rawValue + "\"";
        return transformPrimitive(parameter, stringified);
      }
    }
  }

  protected abstract String[] getArrayValues(Parameter parameter, String rawValue);

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be an array.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transformArray(Parameter parameter, String rawValue) {
    if (rawValue.isEmpty()) {
      return EMPTY_JSON_ARRAY;
    }
    JsonArray array = new JsonArray();
    for (String value : getArrayValues(parameter, rawValue)) {
      array.add(transformPrimitive(parameter, value));
    }
    return array;
  }

  protected abstract String[] getObjectKeysAndValues(Parameter parameter, String rawValue);

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be an object.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  public Object transformObject(Parameter parameter, String rawValue) {
    if (rawValue.isEmpty()) {
      return EMPTY_JSON_OBJECT;
    }

    String[] keysAndValues = getObjectKeysAndValues(parameter, rawValue);
    if (keysAndValues.length % 2 != 0) {
      throw createInvalidValueFormat(parameter);
    }
    JsonObject object = new JsonObject();
    for (int i = 0; i < keysAndValues.length; i = i + 2) {
      object.put(keysAndValues[i], transformPrimitive(parameter, keysAndValues[i + 1]));
    }
    return object;
  }
}
