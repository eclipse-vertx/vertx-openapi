package io.vertx.openapi.mediatype;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.impl.VendorSpecificJson;
import io.vertx.openapi.mediatype.impl.NoOpAnalyser;
import io.vertx.openapi.validation.ValidationContext;
import java.util.List;

/**
 * A MediaTypeRegistration is used to register mediatypes to the openapi mediatype registry. It consists of a predicate
 * that checks if a media type can be handled by the created content analysers.
 */
public interface MediaTypeRegistration extends ContentAnalyserFactory {

  MediaTypeRegistration APPLICATION_JSON =
      create(
          MediaTypePredicate.ofExactTypes(
              MediaType.APPLICATION_JSON,
              MediaType.APPLICATION_JSON_UTF8,
              MediaType.APPLICATION_HAL_JSON),
          ContentAnalyserFactory.json());
  MediaTypeRegistration MULTIPART_FORM_DATA =
      create(
          MediaTypePredicate.ofExactTypes(MediaType.MULTIPART_FORM_DATA),
          ContentAnalyserFactory.multipart());
  MediaTypeRegistration TEXT_PLAIN =
      alwaysValid(
          MediaTypePredicate.ofExactTypes(
              MediaType.TEXT_PLAIN,
              MediaType.TEXT_PLAIN_UTF8));
  MediaTypeRegistration APPLICATION_OCTET_STREAM =
      alwaysValid(
          MediaTypePredicate.ofExactTypes(MediaType.APPLICATION_OCTET_STREAM));
  MediaTypeRegistration VENDOR_SPECIFIC_JSON =
      alwaysValid(
          MediaTypePredicate.ofRegexp(VendorSpecificJson.VENDOR_SPECIFIC_JSON.pattern()));

  /**
   * Creates a new registration from the provided predicate and ContentAnalyserFactory.
   *
   * @param canHandleMediaType A predicate to check if the mediatype can be handled
   * @param factory            A factory for content analysers
   * @return The registration object
   */
  static MediaTypeRegistration create(
      MediaTypePredicate canHandleMediaType,
      ContentAnalyserFactory factory) {
    return new MediaTypeRegistration() {

      @Override
      public List<String> supportedTypes() {
        return canHandleMediaType.supportedTypes();
      }

      @Override
      public ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context) {
        return factory.createContentAnalyser(contentType, content, context);
      }

      @Override
      public boolean canHandle(MediaTypeInfo mediaType) {
        return canHandleMediaType.test(mediaType);
      }

    };
  }

  /**
   * Creates a registration that does simply return the input without any validation. Can be used to register
   * mediatypes that do not require validation.
   *
   * @param canHandleMediaType A predicate to check if the mediatype can be handled
   * @return A registration object.
   */
  static MediaTypeRegistration alwaysValid(MediaTypePredicate canHandleMediaType) {
    return create(canHandleMediaType, NoOpAnalyser::new);
  }

  /**
   * Checks if this registration can handle the given media type. This method is intended to be used by the
   * MediaTypeRegistry.
   *
   * @param mediaType The media type to check
   * @return true if the mediatype can be handled, false otherwise
   */
  boolean canHandle(MediaTypeInfo mediaType);

  /**
   * This method is intended for reporting of supported media types in the system.
   *
   * @return The list of supported types.
   */
  List<String> supportedTypes();

}
