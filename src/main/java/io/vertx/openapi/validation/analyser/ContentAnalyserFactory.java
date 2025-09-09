package io.vertx.openapi.validation.analyser;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.validation.ValidationContext;

/**
 * A factory to create content analysers for custom media types. Two common instances are provided:
 *
 * <li>ContentAnalyserFactory.NO_OP: A content analyser factory that treats input as an opaque octet string</li>
 * <li>ContentAnalyserFactory.JSON: A content analyser factory that treats input as JSON and decodes it for validation</li>
 */
@VertxGen
public interface ContentAnalyserFactory {
  /**
   * A factory creating no-op content analysers that treat content as an opaque octet string. It can be used for content
   * types that do not require or support schema validation.
   */
  ContentAnalyserFactory NO_OP = ContentAnalyser.NoOpAnalyser::new;

  /**
   * A factory creating content analysers that treat the content as JSON and decode it for schema validation.
   */
  ContentAnalyserFactory JSON = ApplicationJsonAnalyser::new;

  /**
   * Returns a content analyser for the given content type, working over the given content in the context of a response
   * or request, as indicated by the context parameter.
   *
   * @param contentType the content type to return an analyzer for
   * @param content the raw request or response body content
   * @param context the validation context (REQUEST or RESPONSE)
   * @return a content analyser for the given content type and content
   */
  @GenIgnore
  ContentAnalyser getContentAnalyser(String contentType, Buffer content, ValidationContext context);
}
