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

import io.vertx.codegen.annotations.VertxGen;

/**
 * Represents an OAuth Flows Object.
 */
@VertxGen
public interface OAuthFlows extends OpenAPIObject {

  /**
   * Configuration for the OAuth Implicit flow.
   */
  OAuthFlow getImplicit();

  /**
   * Configuration for the OAuth Resource Owner Password flow
   */
  OAuthFlow getPassword();

  /**
   * Configuration for the OAuth Client Credentials flow. Previously called application in OpenAPI 2.0.
   */
  OAuthFlow getClientCredentials();

  /**
   * Configuration for the OAuth Authorization Code flow. Previously called accessCode in OpenAPI 2.0.
   */
  OAuthFlow getAuthorizationCode();
}
