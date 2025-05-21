package io.vertx.openapi.mediatype;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.mediatype.impl.ApplicationJsonAnalyser;
import io.vertx.openapi.mediatype.impl.MultipartFormAnalyser;
import io.vertx.openapi.mediatype.impl.NoOpAnalyser;
import io.vertx.openapi.validation.ValidationContext;

@VertxGen
public interface ContentAnalyserFactory {

  /**
   * Creates a new ContentAnalyser for the provided request.
   *
   * @param contentType The raw content type from the http headers.
   * @param content     The content of the request or response.
   * @param context     Whether the analyser is for a request or response.
   * @return A fresh content analyser instance.
   */
  ContentAnalyser createContentAnalyser(String contentType, Buffer content, ValidationContext context);

  static ContentAnalyserFactory json() {
    return ApplicationJsonAnalyser::new;
  }

  static ContentAnalyserFactory noop() {
    return NoOpAnalyser::new;
  }

  static ContentAnalyserFactory multipart() {
    return MultipartFormAnalyser::new;
  }
}
