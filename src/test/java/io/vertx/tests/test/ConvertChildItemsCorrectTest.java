package io.vertx.tests.test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.tests.ResourceHelper.getRelatedTestResourcePath;
import static java.util.stream.Collectors.toList;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.validation.ValidatableResponse;
import io.vertx.openapi.validation.ValidatedRequest;
import io.vertx.tests.test.base.ContractTestBase;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConvertChildItemsCorrectTest extends ContractTestBase {
  private Path CONTRACT_FILE = getRelatedTestResourcePath(PhoneNumberTest.class)
      .resolve("contract_various_scenarios.yaml");

  @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
  @Test
  @DisplayName("Test that array items are transformed correctly")
  void testArrayItemsShouldBeTransformed(VertxTestContext testContext) {
    String operationId = "arrayItems";
    String queryParam = "entityId";
    List<String> expectedValues = List.of("234534", "123452");

    Function<ValidatedRequest, ValidatableResponse> requestProcessor = req -> {
      assertThat(req.getQuery().get(queryParam).getJsonArray()).isEqualTo(new JsonArray(expectedValues));
      return ValidatableResponse.create(200);
    };

    Consumer<HttpClientResponse> responseVerifier = resp -> testContext.verify(() -> {
      assertThat(resp.statusCode()).isEqualTo(200);
      testContext.completeNow();
    });

    loadContract(CONTRACT_FILE, testContext)
        .compose(v -> createServerWithRequestProcessor(requestProcessor, operationId, testContext))
        .compose(v -> {
          List<String> queryValues = expectedValues.stream().map(s -> queryParam + "=" + s).collect(toList());
          String query = String.join("&", queryValues);
          Future<HttpClientRequest> req = createRequest(POST, "/arrayItems?" + query, reqOpts -> {});
          return sendAndVerifyRequest(req, responseVerifier, testContext);
        }).onFailure(testContext::failNow);
  }
}
