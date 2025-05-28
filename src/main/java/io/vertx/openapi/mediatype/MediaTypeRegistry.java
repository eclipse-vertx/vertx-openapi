package io.vertx.openapi.mediatype;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistry;
import io.vertx.openapi.validation.ValidationContext;

/**
 * The MediaTypeRegistry contains all supported MediaTypes and Validators for the mediatypes. New MediaTypes can be registered
 * by providing new MediaTypeRegistrations.
 */
public interface MediaTypeRegistry {
  /**
   * Creates a default registry with application/json, application/multipart and text/plain mediatypes registered.
   *
   * @return A registry with default options.
   */
  static DefaultMediaTypeRegistry createDefault() {
    return new DefaultMediaTypeRegistry()
      .register(MediaTypeRegistration.TEXT_PLAIN)
      .register(MediaTypeRegistration.MULTIPART_FORM_DATA)
      .register(MediaTypeRegistration.APPLICATION_JSON)
      .register(MediaTypeRegistration.APPLICATION_OCTET_STREAM);
  }

  /**
   * Creates an empty registry.
   *
   * @return An empty registry.
   */
  static DefaultMediaTypeRegistry createEmpty() {
    return new DefaultMediaTypeRegistry();
  }

  /**
   * Registers a new MediaTypeHandler
   *
   * @param registration The mediatype registration.
   * @return This registry for a fluent interface.
   */
  DefaultMediaTypeRegistry register(MediaTypeRegistration registration);

  /**
   * Checks if the provided media type is supported by the registration
   *
   * @param type The raw mediatype string
   * @return true if it supported, false otherwise
   */
  boolean isSupported(String type);

  /**
   * Creates a new ContentAnalyser for the provided request.
   *
   * @param contentType The raw content type from the http headers.
   * @param content     The content of the request or response.
   * @param context     Whether the analyser is for a request or response.
   * @return A fresh content analyser instance.
   */
  ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context);
}
