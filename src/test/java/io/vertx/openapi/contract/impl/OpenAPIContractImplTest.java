package io.vertx.openapi.contract.impl;

import io.vertx.openapi.contract.OpenAPIContractException;
import io.vertx.openapi.Utils;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;
import io.vertx.openapi.contract.impl.PathImpl;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAPIContractImplTest {

  private static final List<PathImpl> PATHS_UNSORTED = Arrays.asList(
    new PathImpl("/v2", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/{foo}}/bar", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/docId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/pets/petId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/{docId}", Utils.EMPTY_JSON_OBJECT)
  );

  private static final List<PathImpl> PATHS_SORTED = Arrays.asList(
    new PathImpl("/pets/petId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/docId", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v2", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/v1/docs/{docId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
    new PathImpl("/{abc}/{foo}}/bar", Utils.EMPTY_JSON_OBJECT)
  );

  private static Stream<Arguments> testApplyMountOrderThrows() {
    return Stream.of(
      Arguments.of("a duplicate has been found", Arrays.asList(
        new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
        new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT)
      ), "Found Path duplicate: /pets/{petId}"),
      Arguments.of("paths with same hierarchy but different templated names has been found", Arrays.asList(
        new PathImpl("/pets/{petId}", Utils.EMPTY_JSON_OBJECT),
        new PathImpl("/pets/{foo}", Utils.EMPTY_JSON_OBJECT)
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
}
