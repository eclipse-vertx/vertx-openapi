package io.vertx.tests.contract;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.OpenAPIContractBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests the OpenAPIContractBuilder. Only tests the different constellations a contract can be built from.
 */
@ExtendWith(VertxExtension.class)
public class OpenAPIContractBuilderTest {


  @Test
  void shouldFailWhenNoContractIsProvided(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
      .build()
      .onComplete(ctx.failing(t -> ctx.verify(() -> {
        assertThat(t).isInstanceOf(OpenAPIContractBuilder.OpenAPIContractBuilderException.class);
        assertThat(t).hasMessageThat().isEqualTo("Neither a contract file or a contract is set. One of them must be set.");
        ctx.completeNow();
      })));
  }

  @Test
  void shouldFailWhenFileAndContractAreProvidedContractFirst(Vertx vertx) {
    Assertions.assertThrows(
      OpenAPIContractBuilder.OpenAPIContractBuilderException.class,
      () -> OpenAPIContract.builder(vertx)
        .contract("test")
        .contract(JsonObject.of()));
  }

  @Test
  void shouldFailWhenFileAndContractAreProvidedFileFirst(Vertx vertx) {
    Assertions.assertThrows(
      OpenAPIContractBuilder.OpenAPIContractBuilderException.class,
      () -> OpenAPIContract.builder(vertx)
        .contract(JsonObject.of())
        .contract("test")
    );
  }

  @Test
  void shouldFailWhenMultipleAdditionalContractFilesUseTheSameKey(Vertx vertx) {
    Assertions.assertThrows(
      OpenAPIContractBuilder.OpenAPIContractBuilderException.class,
      () -> OpenAPIContract.builder(vertx)
        .contract("test")
        .addAdditionalContent("k1", "k1")
        .addAdditionalContent("k1", "k2")
    );
  }

  @Test
  void shouldFailWhenMultipleAdditionalContractsUseTheSameKey(Vertx vertx) {
    Assertions.assertThrows(
      OpenAPIContractBuilder.OpenAPIContractBuilderException.class,
      () -> OpenAPIContract.builder(vertx)
        .contract("test")
        .addAdditionalContent("k1", JsonObject.of())
        .addAdditionalContent("k1", JsonObject.of())
    );
  }

  @Test
  void shouldFailWhenMultipleAdditionalContractsAndFilesUseTheSameKey(Vertx vertx) {
    Assertions.assertThrows(
      OpenAPIContractBuilder.OpenAPIContractBuilderException.class,
      () -> OpenAPIContract.builder(vertx)
        .contract("test")
        .addAdditionalContent("k1", JsonObject.of())
        .addAdditionalContent("k1", "f1")
    );
  }

  @Test
  void shouldCreateContractWhenValidContractFileIsProvided(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
      .contract("v3.1/petstore.json")
      .build()
      .onComplete(ctx.succeedingThenComplete());
  }


  @Test
  void shouldCreateContractWhenValidContractIsProvided(Vertx vertx, VertxTestContext ctx) {
    var contract = vertx.fileSystem().readFileBlocking("v3.1/petstore.json").toJsonObject();
    OpenAPIContract.builder(vertx)
      .contract(contract)
      .build()
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void shouldCreateContractWhenValidContractFileAndAdditionalFilesAreProvided(Vertx vertx, VertxTestContext ctx) {
    OpenAPIContract.builder(vertx)
      .contract("io/vertx/tests/contract/from_with_path_and_additional_files/petstore.json")
      .addAdditionalContent("https://example.com/petstore", "io/vertx/tests/contract/from_with_path_and_additional_files/components.json")
      .build()
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void shouldCreateContractWhenValidContractAndAdditionalFilesAreProvided(Vertx vertx, VertxTestContext ctx) {
    var contract = vertx.fileSystem().readFileBlocking("io/vertx/tests/contract/from_with_path_and_additional_files/petstore.json").toJsonObject();
    OpenAPIContract.builder(vertx)
      .contract(contract)
      .addAdditionalContent("https://example.com/petstore", "io/vertx/tests/contract/from_with_path_and_additional_files/components.json")
      .build()
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  void shouldCreateContractWhenValidContractFileAndAdditionalContentAreProvided(Vertx vertx, VertxTestContext ctx) {
    var components = vertx.fileSystem().readFileBlocking("io/vertx/tests/contract/from_with_path_and_additional_files/components.json").toJsonObject();
    OpenAPIContract.builder(vertx)
      .contract("io/vertx/tests/contract/from_with_path_and_additional_files/petstore.json")
      .addAdditionalContent("https://example.com/petstore", components)
      .build()
      .onComplete(ctx.succeedingThenComplete());
  }


}
