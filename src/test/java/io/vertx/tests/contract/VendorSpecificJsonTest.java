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

package io.vertx.tests.contract;

import io.vertx.openapi.contract.VendorSpecificJson;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class VendorSpecificJsonTest {

  @Test
  void testValidVendorSpecificJson() {
    assertThat(VendorSpecificJson.matches("application/vnd.kafka.binary+json")).isTrue();
    // not providing a content type (null or empty)
    assertThat(VendorSpecificJson.matches(null)).isFalse();
    assertThat(VendorSpecificJson.matches("")).isFalse();
    // missing vnd. prefix
    assertThat(VendorSpecificJson.matches("application/kafka.binary+json")).isFalse();
    // missing +json suffix
    assertThat(VendorSpecificJson.matches("application/vnd.kafka.binary")).isFalse();
    // missing content type class
    assertThat(VendorSpecificJson.matches("vnd.kafka.binary+json")).isFalse();
    // wrong formatting
    assertThat(VendorSpecificJson.matches("application\\vnd.kafka.binary+json")).isFalse();
    assertThat(VendorSpecificJson.matches("application/vnd_kafka.binary+json")).isFalse();
    assertThat(VendorSpecificJson.matches("application/vnd.kafka.binary-json")).isFalse();
  }
}
