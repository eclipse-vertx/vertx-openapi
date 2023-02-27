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

package io.vertx.openapi.contract.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.impl.Utils;
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
import static io.vertx.openapi.ResourceHelper.TEST_RESOURCE_PATH;
import static io.vertx.openapi.contract.ContractErrorType.UNSUPPORTED_FEATURE;
import static io.vertx.openapi.contract.OpenAPIVersion.V3_1;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAPIContractImplTest {

  private static final String BASE_PATH = "";

  private static final List<PathImpl> PATHS_UNSORTED = Arrays.asList(
    new PathImpl(BASE_PATH, "/v2", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/{abc}/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/{abc}/{foo}/bar", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/v1/docs/docId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/pets/petId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/v1/docs/{docId}", Utils.EMPTY_JSON_OBJECT)
  );

  private static final List<PathImpl> PATHS_SORTED = Arrays.asList(
    new PathImpl(BASE_PATH, "/pets/petId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/v1/docs/docId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/v2", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/v1/docs/{docId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/{abc}/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl(BASE_PATH, "/{abc}/{foo}/bar", Utils.EMPTY_JSON_OBJECT)
  );

  private static Stream<Arguments> testApplyMountOrderThrows() {
    return Stream.of(
      Arguments.of("a duplicate has been found", Arrays.asList(
        new PathImpl(BASE_PATH, "/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
        new PathImpl(BASE_PATH, "/pets/{petId}", Utils.EMPTY_JSON_OBJECT)
      ), "Found Path duplicate: /pets/{petId}"),
      Arguments.of("paths with same hierarchy but different templated names has been found", Arrays.asList(
        new PathImpl(BASE_PATH, "/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
        new PathImpl(BASE_PATH, "/pets/{foo}", Utils.EMPTY_JSON_OBJECT)
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
      assertThrows(OpenAPIContractException.class, () -> new OpenAPIContractImpl(contract, null, null));
    String expectedMsg =
      "The passed OpenAPI contract contains a feature that is not supported: Different base paths in server urls";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(UNSUPPORTED_FEATURE);
  }

  @Test
  void testBasePath() {
    JsonObject server1 = new JsonObject().put("url", "http://foo.bar/foo");
    JsonObject contractJson = new JsonObject().put("servers", new JsonArray().add(server1));

    OpenAPIContractImpl contract = new OpenAPIContractImpl(contractJson, null, null);
    assertThat(contract.basePath).isEqualTo("/foo");

    OpenAPIContractImpl contractEmpty = new OpenAPIContractImpl(new JsonObject(), null, null);
    assertThat(contractEmpty.basePath).isEqualTo("");
  }

  @Test
  void testGetters() throws IOException {
    Path pathDereferencedContract = TEST_RESOURCE_PATH.resolve("v3.1").resolve("petstore_dereferenced.json");
    JsonObject resolvedSpec = Buffer.buffer(Files.readAllBytes(pathDereferencedContract)).toJsonObject();
    SchemaRepository schemaRepository = Mockito.mock(SchemaRepository.class);
    OpenAPIContractImpl contract = new OpenAPIContractImpl(resolvedSpec, V3_1, schemaRepository);

    assertThat(contract.getServers()).hasSize(1);
    assertThat(contract.getServers().get(0).getURL()).isEqualTo("https://petstore.swagger.io/v1");
    assertThat(contract.getPaths()).hasSize(2);
    assertThat(contract.operations()).hasSize(3);
    Operation showPetById = contract.operation("showPetById");
    assertThat(showPetById).isNotNull();
    assertThat(contract.operation("fooBar")).isNull();
    assertThat(contract.getVersion()).isEqualTo(V3_1);
    assertThat(contract.getRawContract()).isEqualTo(resolvedSpec);
    assertThat(contract.getSchemaRepository()).isEqualTo(schemaRepository);
    assertThat(contract.getSchemaRepository()).isEqualTo(schemaRepository);
    assertThat(contract.findPath("/v1/pets/123").getName()).isEqualTo("/pets/{petId}");
    assertThat(contract.findOperation("/v1/pets/123", GET)).isEqualTo(showPetById);

    assertThat(contract.findOperation("/v1/pets/123/134", GET)).isNull();
    assertThat(contract.findOperation("/v1/pets/123", PATCH)).isNull();
  }
}
