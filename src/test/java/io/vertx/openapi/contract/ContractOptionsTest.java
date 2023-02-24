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

package io.vertx.openapi.contract;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class ContractOptionsTest {

  private final String basePath = "/base/";

  @Test
  void testConstructors() {
    ContractOptions co = new ContractOptions(new JsonObject().put("basePath", basePath));
    assertThat(co.getBasePath()).isEqualTo(basePath);

    ContractOptions co2 = new ContractOptions(co);
    assertThat(co.getBasePath()).isEqualTo(co2.getBasePath());
    assertThat(co).isNotSameInstanceAs(co2);
  }

  @Test
  void testSetterGetter() {
    ContractOptions co = new ContractOptions();
    assertThat(co.getBasePath()).isNull();
    co.setBasePath(basePath);
    assertThat(co.getBasePath()).isEqualTo(basePath);
  }

  @Test
  void testToJson() {
    ContractOptions co = new ContractOptions().setBasePath(basePath);
    assertThat(co.toJson()).isEqualTo(new JsonObject().put("basePath", basePath));
  }

  @Test
  void testToString() {
    ContractOptions co = new ContractOptions().setBasePath(basePath);
    assertThat(co.toString()).isEqualTo("{\"basePath\":\"/base/\"}");
  }
}
