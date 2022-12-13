package io.vertx.openapi.validation;

import java.util.Map;

interface Parameters {
  /**
   * @return the cookie parameters.
   */
  Map<String, RequestParameter> getCookies();

  /**
   * @return the header parameters.
   */
  Map<String, RequestParameter> getHeaders();

  /**
   * @return the body.
   */
  RequestParameter getBody();
}
