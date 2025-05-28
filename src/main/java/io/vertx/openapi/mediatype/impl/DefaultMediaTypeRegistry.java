package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.*;
import io.vertx.openapi.validation.ValidationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for MediaTypeRegistry
 */
public class DefaultMediaTypeRegistry implements MediaTypeRegistry {
  private final List<MediaTypeRegistration> registrations = new ArrayList<>();


  @Override
  public DefaultMediaTypeRegistry register(MediaTypeRegistration registration) {
    this.registrations.add(registration);
    return this;
  }

  @Override
  public boolean isSupported(String type) {
    var v = MediaType.of(type);
    return this.registrations.stream().anyMatch(x -> x.canHandle(v.fullType()));
  }

  @Override
  public ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context) {
    var v = MediaType.of(contentType);
    var reg = this.registrations.stream().filter(x -> x.canHandle(v.fullType())).findFirst();
    if (reg.isEmpty()) {
      throw new MediaTypeException("Unsupported media type " + contentType);
    }
    return reg.get().createContentAnalyser(contentType, content, context);
  }
}
