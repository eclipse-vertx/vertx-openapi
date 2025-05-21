package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.ContentAnalyser;
import io.vertx.openapi.mediatype.MediaTypeException;
import io.vertx.openapi.mediatype.MediaTypeInfo;
import io.vertx.openapi.mediatype.MediaTypeRegistration;
import io.vertx.openapi.mediatype.MediaTypeRegistry;
import io.vertx.openapi.validation.ValidationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    var v = MediaTypeInfo.of(type);
    return this.registrations.stream().anyMatch(x -> x.canHandle(v));
  }

  @Override
  public ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context) {
    var v = MediaTypeInfo.of(contentType);
    var reg = this.registrations.stream().filter(x -> x.canHandle(v)).findFirst();
    if (reg.isEmpty()) {
      throw new MediaTypeException("Unsupported media type " + contentType);
    }
    return reg.get().createContentAnalyser(contentType, content, context);
  }

  @Override
  public List<String> supportedTypes() {
    return registrations.stream().flatMap(x -> x.supportedTypes().stream()).collect(Collectors.toList());
  }
}
