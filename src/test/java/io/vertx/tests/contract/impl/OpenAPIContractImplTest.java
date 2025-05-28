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

package io.vertx.tests.contract.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;
import io.vertx.openapi.contract.impl.PathImpl;
import io.vertx.openapi.mediatype.MediaTypeRegistry;
import io.vertx.tests.ResourceHelper;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Operation;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PATCH;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_FEATURE;
import static io.vertx.openapi.contract.OpenAPIVersion.V3_1;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAPIContractImplTest {

  private static final String BASE_PATH = "";

  private static final Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(OpenAPIContractImplTest.class);
  private static final Path VALID_CONTRACTS_JSON = RESOURCE_PATH.resolve("contract_valid.json");

  private static final List<PathImpl> PATHS_UNSORTED = Arrays.asList(
    new PathImpl(BASE_PATH, "/v2", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/{abc}/pets/{petId}", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/{abc}/{foo}/bar", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/pets/{petId}", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/v1/docs/docId", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/pets/petId", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/v1/docs/{docId}", EMPTY_JSON_OBJECT, emptyList())
  );

  private static final List<PathImpl> PATHS_SORTED = Arrays.asList(
    new PathImpl(BASE_PATH, "/pets/petId", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/v1/docs/docId", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/v2", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/pets/{petId}", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/v1/docs/{docId}", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/{abc}/pets/{petId}", EMPTY_JSON_OBJECT, emptyList()),
    new PathImpl(BASE_PATH, "/{abc}/{foo}/bar", EMPTY_JSON_OBJECT, emptyList())
  );

  private static Stream<Arguments> testApplyMountOrderThrows() {
    return Stream.of(
      Arguments.of("a duplicate has been found", Arrays.asList(
        new PathImpl(BASE_PATH, "/pets/{petId}", EMPTY_JSON_OBJECT, emptyList()),
        new PathImpl(BASE_PATH, "/pets/{petId}", EMPTY_JSON_OBJECT, emptyList())
      ), "Found Path duplicate: /pets/{petId}"),
      Arguments.of("paths with same hierarchy but different templated names has been found", Arrays.asList(
        new PathImpl(BASE_PATH, "/pets/{petId}", EMPTY_JSON_OBJECT, emptyList()),
        new PathImpl(BASE_PATH, "/pets/{foo}", EMPTY_JSON_OBJECT, emptyList())
      ), "Found Paths with same hierarchy but different templated names: /pets/{}")
    );
  }

  @RepeatedTest(10)
  void testApplyMountOrder() {
    List<PathImpl> shuffled = new ArrayList<>(PATHS_UNSORTED);
    Collections.shuffle(shuffled);
    List<PathImpl> sorted = OpenAPIContractImpl.applyMountOrder(new ArrayList<>(PATHS_UNSORTED));
    for (int x = 0; x < PATHS_SORTED.size(); x++) {
      assertThat(PATHS_SORTED.get(x).getName()).isEqualTo(sorted.get(x).getName());
    }
  }

  @ParameterizedTest(name = "{index} applyMountOrder should throw error when {0}")
  @MethodSource
  void testApplyMountOrderThrows(String scenario, List<PathImpl> paths, String expectedReason) {
    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> OpenAPIContractImpl.applyMountOrder(paths));
    String expectedMsg = "The passed OpenAPI contract is invalid: " + expectedReason;
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
  }

  @Test
  void testDifferentBasePaths() {
    JsonObject server1 = new JsonObject().put("url", "http://foo.bar/foo");
    JsonObject server2 = new JsonObject().put("url", "http://foo.bar/foobar");
    JsonObject contract = new JsonObject().put("servers", new JsonArray().add(server1).add(server2));

    OpenAPIContractException exception =
      assertThrows(OpenAPIContractException.class, () -> new OpenAPIContractImpl(contract, null, null, null));
    String expectedMsg =
      "The passed OpenAPI contract contains a feature that is not supported: Different base paths in server urls";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(UNSUPPORTED_FEATURE);
  }

  @Test
  void testBasePath() {
    JsonObject server1 = new JsonObject().put("url", "http://foo.bar/foo");
    JsonObject contractJson = new JsonObject().put("servers", new JsonArray().add(server1));

    OpenAPIContractImpl contract = new OpenAPIContractImpl(contractJson, null, null, null);
    assertThat(contract.basePath()).isEqualTo("/foo");

    OpenAPIContractImpl contractEmpty = new OpenAPIContractImpl(new JsonObject(), null, null, null);
    assertThat(contractEmpty.basePath()).isEqualTo("");
  }

  @Test
  void testGetters() throws IOException {
    JsonObject testDataObject =
      Buffer.buffer(Files.readAllBytes(VALID_CONTRACTS_JSON)).toJsonObject().getJsonObject("0000_Test_Getters");
    JsonObject resolvedSpec = testDataObject.getJsonObject("contractModel");
    SchemaRepository schemaRepository = Mockito.mock(SchemaRepository.class);
    OpenAPIContractImpl contract = new OpenAPIContractImpl(resolvedSpec, V3_1, schemaRepository, MediaTypeRegistry.createDefault());

    assertThat(contract.getServers()).hasSize(1);
    assertThat(contract.getServers().get(0).getURL()).isEqualTo("https://petstore.swagger.io/v1");
    assertThat(contract.getSecurityRequirements()).hasSize(1);
    assertThat(contract.getSecurityRequirements().get(0).getNames()).containsExactly("BasicAuth");
    assertThat(contract.getPaths()).hasSize(2);
    assertThat(contract.operations()).hasSize(3);
    Operation showPetById = contract.operation("showPetById");
    assertThat(showPetById).isNotNull();
    assertThat(showPetById.getSecurityRequirements()).hasSize(1);
    assertThat(showPetById.getSecurityRequirements().get(0).getNames()).containsExactly("BasicAuth");
    assertThat(contract.operation("fooBar")).isNull();
    assertThat(contract.getVersion()).isEqualTo(V3_1);
    assertThat(contract.getRawContract()).isEqualTo(resolvedSpec);
    assertThat(contract.getSchemaRepository()).isEqualTo(schemaRepository);
    assertThat(contract.getSchemaRepository()).isEqualTo(schemaRepository);
    assertThat(contract.findPath("/v1/pets/123").getName()).isEqualTo("/pets/{petId}");
    assertThat(contract.findOperation("/v1/pets/123", GET)).isEqualTo(showPetById);

    assertThat(contract.findOperation("/v1/pets/123/134", GET)).isNull();
    assertThat(contract.findOperation("/v1/pets/123", PATCH)).isNull();
    assertThat(contract.securityScheme("BasicAuth")).isNotNull();
  }

  @Test
  void testGettersEmptySecurityRequirements() throws IOException {
    OpenAPIContractImpl contract = fromTestData("0001_Getters_No_Security_Requirements");
    assertThat(contract.getSecurityRequirements()).isEmpty();

    Operation showPetById = contract.operation("listPets");
    assertThat(showPetById).isNotNull();
    assertThat(showPetById.getSecurityRequirements()).isEmpty();
  }

  private static OpenAPIContractImpl fromTestData(String testId) throws IOException {
    JsonObject testDataObject =
      Buffer.buffer(Files.readAllBytes(VALID_CONTRACTS_JSON)).toJsonObject().getJsonObject(testId);
    JsonObject resolvedSpec = testDataObject.getJsonObject("contractModel");
    SchemaRepository schemaRepository = Mockito.mock(SchemaRepository.class);
    return new OpenAPIContractImpl(resolvedSpec, V3_1, schemaRepository, MediaTypeRegistry.createDefault());
  }
}
