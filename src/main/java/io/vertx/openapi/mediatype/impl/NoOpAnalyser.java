package io.vertx.openapi.mediatype.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.openapi.validation.ValidationContext;

public class NoOpAnalyser extends AbstractContentAnalyser {
  public NoOpAnalyser(String contentType, Buffer content, ValidationContext context) {
    super(contentType, content, context);
  }

  @Override
  public void checkSyntacticalCorrectness() {
    // no syntax check
  }

  @Override
  public Object transform() {
    return content;
  }
}
