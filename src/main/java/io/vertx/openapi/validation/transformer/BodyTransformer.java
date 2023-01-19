package io.vertx.openapi.validation.transformer;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.contract.MediaType;

public interface BodyTransformer {
  
  Object transform(MediaType type, Buffer value);
}
