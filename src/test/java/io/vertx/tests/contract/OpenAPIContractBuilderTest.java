/*
 * Copyright (c) 2025, Lukas Jelonek
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

import static com.google.common.truth.Truth.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.OpenAPIContractBuilder;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.impl.Utils;
import io.vertx.tests.ResourceHelper;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the OpenAPIContractBuilder. Only tests the different constellations a contract can be built from.
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class OpenAPIContractBuilderTest {

  @Test
  void should_create_contract_when_valid_contract_path_is_provided(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
        .setContract("v3.1/petstore.json")
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_create_contract_when_valid_contract_is_provided(Vertx vertx, VertxTestContext ctx) {
    var contract = ResourceHelper.loadJson(vertx, Paths.get("v3.1/petstore.json"));
    OpenAPIContract.builder(vertx)
        .setContract(contract)
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_create_contract_when_valid_contract_path_and_additional_contract_paths_are_provided(Vertx vertx,
      VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
        .setContract("io/vertx/tests/contract/from_with_path_and_additional_files/petstore.json")
        .putAdditionalContractPath("https://example.com/petstore",
            "io/vertx/tests/contract/from_with_path_and_additional_files/components.json")
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test

  void should_create_contract_when_valid_contract_and_additional_contract_path_is_provided(Vertx vertx,
      VertxTestContext ctx) {
    var contract = ResourceHelper.loadJson(vertx,
        Paths.get("io/vertx/tests/contract/from_with_path_and_additional_files/petstore.json"));
    OpenAPIContract.builder(vertx)
        .setContract(contract)
        .putAdditionalContractPath("https://example.com/petstore",
            "io/vertx/tests/contract/from_with_path_and_additional_files/components.json")
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test

  void should_create_contract_when_valid_contract_path_and_additional_contract_part_is_provided(Vertx vertx,
      VertxTestContext ctx) {
    var components = ResourceHelper.loadJson(vertx,
        Paths.get("io/vertx/tests/contract/from_with_path_and_additional_files/components.json"));
    OpenAPIContract.builder(vertx)
        .setContract("io/vertx/tests/contract/from_with_path_and_additional_files/petstore.json")
        .putAdditionalContractPart("https://example.com/petstore", components)
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_fail_when_no_contract_or_contract_path_is_provided(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
        .build()
        .onComplete(ctx.failing(t -> ctx.verify(() -> {
          assertThat(t).isInstanceOf(OpenAPIContractBuilder.OpenAPIContractBuilderException.class);
          assertThat(t).hasMessageThat()
              .isEqualTo("Neither a contract path or a contract is set. One of them must be set.");
          ctx.completeNow();
        })));
  }

  @Test
  void should_fail_when_contract_is_invalid(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
        .setContract(JsonObject.of())
        .build()
        .onComplete(ctx.failing(t -> ctx.verify(() -> {
          assertThat(t).isInstanceOf(OpenAPIContractException.class);
          ctx.completeNow();
        })));
  }

  /**
   * To test the override mechanisms for additional contracts we use the following setup: <br>
   * We load a contract and add two additional contracts that exist in two versions, distinguishable
   * by their title. Then we replace one of them with the other version and check if the correct
   * version has been loaded.
   */
  @Nested
  @ExtendWith(VertxExtension.class)
  class TestSetupOfAdditionalContractParts {

    private static final String REF1_ID = "http://example.com/ref1";
    private static final String REF2_ID = "http://example.com/ref2";
    private static final String REF1_1_FILE = "io/vertx/tests/builder/ref1.1.yaml";
    private static final String REF1_2_FILE = "io/vertx/tests/builder/ref1.2.yaml";
    private static final String REF2_1_FILE = "io/vertx/tests/builder/ref2.1.yaml";
    private static final String REF2_2_FILE = "io/vertx/tests/builder/ref2.2.yaml";

    private Vertx vertx;

    @BeforeEach
    void init(Vertx vertx) {
      this.vertx = vertx;

    }

    private JsonObject content(String path) {
      return Utils.readYamlOrJson(vertx, path).await();
    }

    @Test
    void set_additional_contract_part_should_override_existing_path(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContract("io/vertx/tests/builder/contract.yaml")
          .putAdditionalContractPath(REF1_ID, REF1_1_FILE)
          .putAdditionalContractPath(REF2_ID, REF2_1_FILE)
          .setAdditionalContractParts(Map.of(REF1_ID, content(REF1_2_FILE)))
          .build()
          .await();
      should_have(c, "ref1.2", "ref2.1");
    }

    @Test
    void put_additional_contract_part_should_override_existing_path(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContract("io/vertx/tests/builder/contract.yaml")
          .putAdditionalContractPath(REF1_ID, REF1_1_FILE)
          .putAdditionalContractPath(REF2_ID, REF2_1_FILE)
          .putAdditionalContractPart(REF1_ID, content(REF1_2_FILE))
          .build()
          .await();
      should_have(c, "ref1.2", "ref2.1");
    }

    @Test
    void set_additional_contract_path_should_override_existing_contract_part(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContract("io/vertx/tests/builder/contract.yaml")
          .putAdditionalContractPart(REF1_ID, content(REF1_1_FILE))
          .putAdditionalContractPart(REF2_ID, content(REF2_1_FILE))
          .setAdditionalContractPaths(Map.of(REF2_ID, REF2_2_FILE))
          .build()
          .await();
      should_have(c, "ref1.1", "ref2.2");
    }

    @Test
    void put_additional_contract_path_should_override_existing_additional_contract_part(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContract("io/vertx/tests/builder/contract.yaml")
          .putAdditionalContractPart(REF1_ID, content(REF1_1_FILE))
          .putAdditionalContractPart(REF2_ID, content(REF2_1_FILE))
          .putAdditionalContractPath(REF2_ID, REF2_2_FILE)
          .build()
          .await();
      should_have(c, "ref1.1", "ref2.2");
    }

    private void should_have(OpenAPIContract contract, String requestDescription, String responseDescription) {
      var c1 = contract.getSchemaRepository().find((REF1_ID));
      var t1 = c1.<JsonObject>get("info").getString("title");
      var c2 = contract.getSchemaRepository().find((REF2_ID));
      var t2 = c2.<JsonObject>get("info").getString("title");
      assertThat(t1).isEqualTo(requestDescription);
      assertThat(t2).isEqualTo(responseDescription);
    }

    @Test
    void set_additional_contract_parts_should_replace_existing_contract_part(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContract("io/vertx/tests/builder/contract.yaml")
          .putAdditionalContractPart(REF1_ID, content(REF1_1_FILE))
          .putAdditionalContractPart(REF2_ID, content(REF2_1_FILE))
          .setAdditionalContractParts(Map.of(REF2_ID, content(REF2_2_FILE)))
          .build()
          .await();
      assertThat(c.getSchemaRepository().find(REF1_ID)).isNull();
    }

    @Test
    void set_additional_contract_paths_should_replace_existing_contract_paths(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContract("io/vertx/tests/builder/contract.yaml")
          .putAdditionalContractPath(REF1_ID, REF1_1_FILE)
          .putAdditionalContractPath(REF2_ID, REF2_1_FILE)
          .setAdditionalContractPaths(Map.of(REF2_ID, REF2_2_FILE))
          .build()
          .await();
      assertThat(c.getSchemaRepository().find(REF1_ID)).isNull();
    }
  }

}
