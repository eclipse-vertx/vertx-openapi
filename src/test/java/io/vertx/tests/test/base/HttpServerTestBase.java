/*
 * Copyright (c) 2023, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.tests.test.base;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("NewClassNamingConvention")
@ExtendWith(VertxExtension.class)
public class HttpServerTestBase {

  protected int port;
  protected Vertx vertx;
  private HttpClient client;

  /**
   * Creates a new HttpServer with the passed requestHandler.
   * <p></p>
   * <b>Note:</b> This method should only be called once during a test.
   *
   * @param requestHandler The related requestHandler
   * @return a succeeded {@link Future} if the server is running, otherwise a failed {@link Future}.
   */
  protected Future<Void> createServer(Handler<HttpServerRequest> requestHandler) {
    return vertx.createHttpServer().requestHandler(requestHandler).listen(0).
      onSuccess(server -> port = server.actualPort()).mapEmpty();
  }

  @BeforeEach
  void setup(Vertx vertx) {
    this.vertx = vertx;
    this.client = vertx.createHttpClient();
  }

  @AfterEach
  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  void tearDown(VertxTestContext testContext) {
    if (vertx != null) {
      vertx.close().onComplete(testContext.succeedingThenComplete());
    } else {
      testContext.completeNow();
    }
  }

  /**
   * Returns a pre-configured HTTP request.
   *
   * @param method The HTTP method of the request
   * @param path   The path of the request
   * @return a pre-configured HTTP request.
   */
  protected Future<HttpClientRequest> createRequest(HttpMethod method, String path) {
    return client.request(new RequestOptions().setPort(port).setHost("localhost").setMethod(method).setURI(path));
  }
}
