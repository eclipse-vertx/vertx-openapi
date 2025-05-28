package io.vertx.openapi.mediatype;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.mediatype.impl.AbstractContentAnalyser;
import io.vertx.openapi.mediatype.impl.ApplicationJsonAnalyser;
import io.vertx.openapi.mediatype.impl.MultipartFormAnalyser;
import io.vertx.openapi.mediatype.impl.NoOpAnalyser;
import io.vertx.openapi.validation.ValidationContext;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * A MediaTypeRegistration is used to register mediatypes to the openapi mediatype registry. It consists of a predicate
 * that checks if a media type can be handled by the created content analysers.
 */
public interface MediaTypeRegistration extends ContentAnalyserFactory {

  MediaTypeRegistration APPLICATION_JSON =
    create(
      whitelist(
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_JSON_UTF8,
        MediaType.APPLICATION_HAL_JSON),
      ApplicationJsonAnalyser::new);
  MediaTypeRegistration MULTIPART_FORM_DATA =
    create(
      whitelist(MediaType.MULTIPART_FORM_DATA),
      MultipartFormAnalyser::new);
  MediaTypeRegistration TEXT_PLAIN =
    alwaysValid(
      whitelist(
        MediaType.TEXT_PLAIN,
        MediaType.TEXT_PLAIN_UTF8));
  MediaTypeRegistration APPLICATION_OCTET_STREAM =
    alwaysValid(
      whitelist(MediaType.APPLICATION_OCTET_STREAM)
    );

  /**
   * Creates a new registration from the provided predicate and ContentAnalyserFactory.
   *
   * @param canHandleMediaType A predicate to check if the mediatype can be handled
   * @param factory            A factory for content analysers
   * @return The registration object
   */
  static MediaTypeRegistration create(
    Predicate<String> canHandleMediaType,
    ContentAnalyserFactory factory) {
    return new MediaTypeRegistration() {

      @Override
      public ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context) {
        return factory.createContentAnalyser(contentType, content, context);
      }

      @Override
      public boolean canHandle(String mediaType) {
        return canHandleMediaType.test(mediaType);
      }

    };
  }

  /**
   * Creates a registration that does simply returns the input without any validation. Can be used to register
   * mediatypes that do not require validation.
   *
   * @param canHandleMediaType A predicate to check if the mediatype can be handled
   * @return A new registration.
   */
  static MediaTypeRegistration alwaysValid(Predicate<String> canHandleMediaType) {
    return new MediaTypeRegistration() {

      @Override
      public AbstractContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context) {
        return new NoOpAnalyser(contentType, content, context);
      }

      @Override
      public boolean canHandle(String mediaType) {
        return canHandleMediaType.test(mediaType);
      }

    };
  }

  /**
   * Checks if this registration can handle the given media type. This method is intended to be used by the
   * MediaTypeRegistry.
   *
   * @param mediaType The media type to check
   * @return true if the mediatype can be handled, false otherwise
   */
  boolean canHandle(String mediaType);


  /**
   * Factory for a whitelist predicate. Checks if the mediatype is equal to one of the types provided.
   *
   * @param types The whitelisting types
   * @return The predicate that checks if the string is part of the whitelist.
   */
  static Predicate<String> whitelist(String... types) {
    return v -> Arrays.asList(types).contains(v);
  }
}
