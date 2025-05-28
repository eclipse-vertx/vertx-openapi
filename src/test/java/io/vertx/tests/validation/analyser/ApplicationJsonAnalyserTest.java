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

package io.vertx.tests.validation.analyser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.mediatype.impl.ApplicationJsonAnalyser;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.openapi.validation.ValidationContext.REQUEST;
import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationJsonAnalyserTest {

  @Test
  void testTransform() {
    JsonObject dummyBody = new JsonObject().put("foo", "bar");
    ApplicationJsonAnalyser analyser = new ApplicationJsonAnalyser(APPLICATION_JSON.toString(), dummyBody.toBuffer(),
      REQUEST);

    analyser.checkSyntacticalCorrectness(); // must always be executed before transform
    assertThat(analyser.transform()).isEqualTo(dummyBody);
  }

  @Test
  void testCheckSyntacticalCorrectnessThrows() {
    ApplicationJsonAnalyser analyser = new ApplicationJsonAnalyser(APPLICATION_JSON.toString(), Buffer.buffer(
      "\"foobar"), REQUEST);

    ValidatorException exception = assertThrows(ValidatorException.class, analyser::checkSyntacticalCorrectness);
    String expectedMsg = "The request body can't be decoded";
    assertThat(exception.type()).isEqualTo(ILLEGAL_VALUE);
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }
}
