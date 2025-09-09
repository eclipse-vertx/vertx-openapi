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
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.OpenAPIContractBuilder;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.impl.MediaTypeImpl;
import io.vertx.openapi.impl.Utils;
import io.vertx.openapi.validation.analyser.ContentAnalyserFactory;
import io.vertx.tests.ResourceHelper;
import io.vertx.tests.validation.impl.RequestValidatorImplTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the OpenAPIContractBuilder. Only tests the different constellations a contract can be built from.
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class OpenAPIContractBuilderTest {

  private static final String CONTRACT_PATH = "v3.1/petstore.json";
  private static final Path BASE_PATH =
      getRelatedTestResourcePath(OpenAPIContractBuilderTest.class).resolve("from_with_path_and_additional_files");
  private static final Path SPLIT_CONTRACT_PATH = BASE_PATH.resolve("petstore.json");
  private static final Path SPLIT_CONTRACT_REFERENCE_PATH = BASE_PATH.resolve("components.json");
  private static final String SPLIT_CONTRACT_REFERENCE_KEY = "https://example.com/petstore";

  @Test
  void should_create_contract_when_valid_contract_path_is_provided(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
        .setContractPath(CONTRACT_PATH)
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_create_contract_when_valid_contract_is_provided(Vertx vertx, VertxTestContext ctx) {
    var contract = ResourceHelper.loadJson(vertx, Paths.get(CONTRACT_PATH));
    OpenAPIContract.builder(vertx)
        .setContract(contract)
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_create_contract_when_valid_contract_path_and_additional_contract_paths_are_provided(Vertx vertx,
      VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
        .setContractPath(SPLIT_CONTRACT_PATH.toString())
        .putAdditionalContractPartPath(SPLIT_CONTRACT_REFERENCE_KEY, SPLIT_CONTRACT_REFERENCE_PATH.toString())
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_create_contract_when_valid_contract_and_additional_contract_path_is_provided(Vertx vertx,
      VertxTestContext ctx) {
    var contract = ResourceHelper.loadJson(vertx,
        Paths.get(SPLIT_CONTRACT_PATH.toString()));
    OpenAPIContract.builder(vertx)
        .setContract(contract)
        .putAdditionalContractPartPath(SPLIT_CONTRACT_REFERENCE_KEY,
            SPLIT_CONTRACT_REFERENCE_PATH.toString())
        .build()
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void should_create_contract_when_valid_contract_path_and_additional_contract_part_is_provided(Vertx vertx,
      VertxTestContext ctx) {
    var components = ResourceHelper.loadJson(vertx, SPLIT_CONTRACT_REFERENCE_PATH);
    OpenAPIContract.builder(vertx)
        .setContractPath(SPLIT_CONTRACT_PATH.toString())
        .putAdditionalContractPart(SPLIT_CONTRACT_REFERENCE_KEY, components)
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
              .isEqualTo("Neither a contract path nor a contract is set. One of them must be set.");
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

    private final Path BASE_PATH =
        getRelatedTestResourcePath(TestSetupOfAdditionalContractParts.class).resolve("builder");
    private final String CONTRACT_FILE = BASE_PATH.resolve("contract.yaml").toString();
    private final String REF1_1_FILE = BASE_PATH.resolve("ref1.1.yaml").toString();
    private final String REF1_2_FILE = BASE_PATH.resolve("ref1.2.yaml").toString();
    private final String REF2_1_FILE = BASE_PATH.resolve("ref2.1.yaml").toString();
    private final String REF2_2_FILE = BASE_PATH.resolve("ref2.2.yaml").toString();

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
          .setContractPath(CONTRACT_FILE)
          .putAdditionalContractPartPath(REF1_ID, REF1_1_FILE)
          .putAdditionalContractPartPath(REF2_ID, REF2_1_FILE)
          .setAdditionalContractParts(Map.of(REF1_ID, content(REF1_2_FILE)))
          .build()
          .await();
      should_have(c, "ref1.2", "ref2.1");
    }

    @Test
    void put_additional_contract_part_should_override_existing_path(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContractPath(CONTRACT_FILE)
          .putAdditionalContractPartPath(REF1_ID, REF1_1_FILE)
          .putAdditionalContractPartPath(REF2_ID, REF2_1_FILE)
          .putAdditionalContractPart(REF1_ID, content(REF1_2_FILE))
          .build()
          .await();
      should_have(c, "ref1.2", "ref2.1");
    }

    @Test
    void set_additional_contract_path_should_override_existing_contract_part(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContractPath(CONTRACT_FILE)
          .putAdditionalContractPart(REF1_ID, content(REF1_1_FILE))
          .putAdditionalContractPart(REF2_ID, content(REF2_1_FILE))
          .setAdditionalContractPartPaths(Map.of(REF2_ID, REF2_2_FILE))
          .build()
          .await();
      should_have(c, "ref1.1", "ref2.2");
    }

    @Test
    void put_additional_contract_path_should_override_existing_additional_contract_part(Vertx vertx) {
      var c = OpenAPIContract.builder(vertx)
          .setContractPath(CONTRACT_FILE)
          .putAdditionalContractPart(REF1_ID, content(REF1_1_FILE))
          .putAdditionalContractPart(REF2_ID, content(REF2_1_FILE))
          .putAdditionalContractPartPath(REF2_ID, REF2_2_FILE)
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
          .setContractPath(CONTRACT_FILE)
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
          .setContractPath(CONTRACT_FILE)
          .putAdditionalContractPartPath(REF1_ID, REF1_1_FILE)
          .putAdditionalContractPartPath(REF2_ID, REF2_1_FILE)
          .setAdditionalContractPartPaths(Map.of(REF2_ID, REF2_2_FILE))
          .build()
          .await();
      assertThat(c.getSchemaRepository().find(REF1_ID)).isNull();
    }
  }

  @Nested
  @ExtendWith(VertxExtension.class)
  class TestAdditionalMediaTypes {
    private final JsonObject CONTRACT =
      new JsonObject(
        "{\n" +
          "  \"openapi\": \"3.1.0\",\n" +
          "  \"info\": {\"version\": \"1.0.0\", \"title\": \"Swagger Petstore\", \"license\": {\"identifier\": \"MIT\", \"name\": \"MIT License\"}},\n" +
          "  \"paths\": {\n" +
          "    \"/pets\": {\n" +
          "      \"get\": {\n" +
          "        \"summary\": \"List Pets\"," +
          "        \"operationId\": \"listPets\"," +
          "        \"responses\": {\n" +
          "          \"default\": {\n" +
          "            \"description\": \"Default Response\",\n" +
          "            \"content\": {\n" +
          "              \"application/yml\": {\n" +
          "                \"schema\": {\n" +
          "                  \"type\": \"object\"\n" +
          "                }\n" +
          "              }\n" +
          "            }\n" +
          "          }\n" +
          "        }\n" +
          "      },\n" +
          "      \"post\": {\n" +
          "        \"summary\": \"Create Pets\"," +
          "        \"operationId\": \"createPets\"," +
          "        \"requestBody\": {\n" +
          "          \"required\": true,\n" +
          "          \"content\": {\n" +
          "            \"application/yaml\": {\n" +
          "              \"schema\": {\n" +
          "                \"type\": \"object\"\n" +
          "              }\n" +
          "            }\n" +
          "          }\n" +
          "        },\n" +
          "        \"responses\": {\n" +
          "          \"default\": {\n" +
          "            \"description\": \"Default Response\",\n" +
          "            \"content\": {\n" +
          "              \"application/json\": {\n" +
          "                \"schema\": {\n" +
          "                  \"type\": \"object\"\n" +
          "                }\n" +
          "              }\n" +
          "            }\n" +
          "          }\n" +
          "        }\n" +
          "      }\n" +
          "    },\n" +
          "    \"/pets/updates\": {\n" +
          "      \"get\": {\n" +
          "        \"summary\": \"Listen to pet update events\"," +
          "        \"operationId\": \"petEvents\"," +
          "        \"responses\": {\n" +
          "          \"default\": {\n" +
          "            \"description\": \"Stream of Pet Events\",\n" +
          "            \"content\": {\n" +
          "              \"text/event-stream\": {}\n" +
          "            }\n" +
          "          }\n" +
          "        }\n" +
          "      }\n" +
          "    }\n" +
          "  }\n" +
          "}"
      );
    @Test
    void should_accept_additional_media_types(Vertx vertx, VertxTestContext ctx) {
      OpenAPIContract.builder(vertx)
          .setContract(CONTRACT)
          .registerSupportedMediaType(
              new RequestValidatorImplTest.YamlContentAnalyzerFactory(),
              "application/yml", "application/yaml"
          )
          .registerUncheckedMediaType("text/event-stream")
          .build()
          .onComplete(ctx.succeeding(c -> ctx.verify(() -> {
            var listPets = c.operation("listPets");

            assertThat(listPets.getDefaultResponse().getContent()).containsKey("application/yml");
            assertThat(listPets.getDefaultResponse().getContent().get("application/yml"))
                .isInstanceOf(MediaTypeImpl.class);
            assertThat(
                ((MediaTypeImpl) listPets.getDefaultResponse().getContent().get("application/yml"))
                    .getContentAnalyserFactory()).isNotNull();
            assertThat(
                ((MediaTypeImpl) listPets.getDefaultResponse().getContent().get("application/yml"))
                    .getContentAnalyserFactory()).isInstanceOf(RequestValidatorImplTest.YamlContentAnalyzerFactory.class);

            var createPets = c.operation("createPets");

            assertThat(createPets.getRequestBody().getContent()).containsKey("application/yaml");
            assertThat(createPets.getRequestBody().getContent().get("application/yaml"))
                .isInstanceOf(MediaTypeImpl.class);
            assertThat(
                ((MediaTypeImpl) createPets.getRequestBody().getContent().get("application/yaml"))
                    .getContentAnalyserFactory()).isNotNull();
            assertThat(
                ((MediaTypeImpl) createPets.getRequestBody().getContent().get("application/yaml"))
                    .getContentAnalyserFactory()).isInstanceOf(RequestValidatorImplTest.YamlContentAnalyzerFactory.class);
            ctx.completeNow();

            var petEvents = c.operation("petEvents");

            assertThat(petEvents.getDefaultResponse().getContent()).containsKey("text/event-stream");
            assertThat(petEvents.getDefaultResponse().getContent().get("text/event-stream"))
              .isInstanceOf(MediaTypeImpl.class);
            assertThat(
              ((MediaTypeImpl) petEvents.getDefaultResponse().getContent().get("text/event-stream"))
                .getContentAnalyserFactory()).isNotNull();
            assertThat(
              ((MediaTypeImpl) petEvents.getDefaultResponse().getContent().get("text/event-stream"))
                .getContentAnalyserFactory()).isEqualTo(ContentAnalyserFactory.NO_OP);
          })));
    }
  }
}
