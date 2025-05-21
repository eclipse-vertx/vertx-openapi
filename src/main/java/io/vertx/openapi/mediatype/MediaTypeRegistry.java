package io.vertx.openapi.mediatype;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistry;
import io.vertx.openapi.validation.ValidationContext;
import java.util.List;

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
  static MediaTypeRegistry createDefault() {
    return new DefaultMediaTypeRegistry()
        .register(MediaTypeRegistration.APPLICATION_JSON)
        .register(MediaTypeRegistration.MULTIPART_FORM_DATA)
        .register(MediaTypeRegistration.APPLICATION_OCTET_STREAM)
        .register(MediaTypeRegistration.TEXT_PLAIN)
        .register(MediaTypeRegistration.VENDOR_SPECIFIC_JSON);
  }

  /**
   * Creates an empty registry.
   *
   * @return An empty registry.
   */
  static MediaTypeRegistry createEmpty() {
    return new DefaultMediaTypeRegistry();
  }

  /**
   * Registers a new MediaTypeHandler
   *
   * @param registration The mediatype registration.
   * @return This registry for a fluent interface.
   */
  MediaTypeRegistry register(MediaTypeRegistration registration);

  /**
   * Checks if the provided media type is supported by the registration
   *
   * @param type The raw mediatype string
   * @return true if it supported, false otherwise
   */
  boolean isSupported(String type);

  /**
   * @return A list of all supported types.
   */
  List<String> supportedTypes();

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
