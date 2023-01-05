package io.vertx.openapi.validation.validator.transformer;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.openapi.contract.Parameter;

import static io.vertx.openapi.validation.ValidatorException.createCantDecodeValue;

public interface ValueTransformer {

  /**
   * Transforms the raw value from its {@link String} representation into JSON. This method does not only decode a
   * {@link String}, it also takes the different {@link io.vertx.openapi.contract.Style styles} into account}.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  default Object transform(Parameter parameter, String rawValue) {
    try {
      switch (parameter.getSchema().<String>get("type")) {
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
  default Object transformPrimitive(Parameter parameter, String rawValue) {
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

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be an object.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  Object transformObject(Parameter parameter, String rawValue);

  /**
   * Like {@link #transform(Parameter, String)}, but only for values considered to be an array.
   *
   * @param parameter The parameter model
   * @param rawValue  The parameter value
   * @return An {@link Object} holding the transformed value.
   */
  Object transformArray(Parameter parameter, String rawValue);
}
