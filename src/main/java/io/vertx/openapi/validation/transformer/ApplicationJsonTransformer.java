package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;

public class ApplicationJsonTransformer implements BodyTransformer {
  @Override
  public Object transform(MediaType type, Buffer value) {
    try {
      return Json.decodeValue(value);
    } catch (DecodeException e) {
      throw new ValidatorException("The request body can't be decoded", ILLEGAL_VALUE);
    }
  }
}
