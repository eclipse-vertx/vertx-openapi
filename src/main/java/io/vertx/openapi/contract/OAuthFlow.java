/*
 * Copyright 2023 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import java.util.Set;

/**
 * Represents an OAuth Flow Object (the configuration).
 */
@VertxGen
public interface OAuthFlow extends OpenAPIObject {

  /**
   * The authorization URL to be used for this flow. This MUST be in the form of a URL.
   * The OAuth2 standard requires the use of TLS.
   */
  String getAuthorizationUrl();

  /**
   * The token URL to be used for this flow. This MUST be in the form of a URL.
   * The OAuth2 standard requires the use of TLS.
   */
  String getTokenUrl();

  /**
   * The URL to be used for obtaining refresh tokens. This MUST be in the form of a URL.
   * The OAuth2 standard requires the use of TLS.
   */
  @Nullable
  String getRefreshUrl();

  /**
   * The available scopes for the OAuth2 security scheme. A set of the scope names.
   * The set MAY be empty.
   */
  Set<String> getScopes();
}
