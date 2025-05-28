package io.vertx.openapi.mediatype;

import io.vertx.openapi.validation.ValidatorException;

/**
 * The content analyser is responsible to check if a request or response has the correct format i.e. is syntactically
 * correct, and to transform the content-buffer into a representation that can be validated against a schema.
 * <p>
 * These two methods are intentionally bundled in {@link ContentAnalyser} to prevent some operations from having to
 * be performed twice. This is particularly helpful if a library is used that cannot distinguish between these steps.
 * In this case, an intermediate result that was generated in {@link #checkSyntacticalCorrectness()}, for example,
 * can be reused.
 * <p>
 * Therefore, it is very important to ensure that the {@link #checkSyntacticalCorrectness()} method is always called
 * before.
 */
public interface ContentAnalyser {
  /**
   * Checks if the content has the expected format i.e. is syntactically correct.
   * <p>
   * Throws a {@link ValidatorException} if the content is syntactically incorrect.
   */
  void checkSyntacticalCorrectness() throws ValidatorException;

  /**
   * Transforms the content into a format that can be validated by the
   * {@link io.vertx.openapi.validation.RequestValidator}, or {@link io.vertx.openapi.validation.ResponseValidator}.
   * <p>
   * Throws a {@link ValidatorException} if the content can't be transformed.
   *
   * @return the transformed content.
   */
  Object transform();
}
