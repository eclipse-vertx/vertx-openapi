package io.vertx.openapi.objects.impl;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.openapi.ResourceHelper;
import io.vertx.openapi.objects.Parameter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.objects.Location.PATH;
import static io.vertx.openapi.objects.impl.ParameterImpl.parseParameters;
import static java.util.concurrent.TimeUnit.SECONDS;

@ExtendWith(VertxExtension.class)
class OperationImplTest {
  private static final Path RESOURCE_PATH = ResourceHelper.getRelatedTestResourcePath(OperationImplTest.class);

  private static final Path VALID_OPERATIONS_JSON = RESOURCE_PATH.resolve("operation_valid.json");

  private static JsonObject validTestData;

  @BeforeAll
  @Timeout(value = 2, timeUnit = SECONDS)
  static void setUp(Vertx vertx) {
    validTestData = vertx.fileSystem().readFileBlocking(VALID_OPERATIONS_JSON.toString()).toJsonObject();
  }

  private static OperationImpl fromTestData(String id, JsonObject testData) {
    JsonObject testDataObject = testData.getJsonObject(id);
    String path = testDataObject.getString("path");
    HttpMethod method = HttpMethod.valueOf(testDataObject.getString("method").toUpperCase());
    JsonObject operationModel = testDataObject.getJsonObject("operationModel");
    List<Parameter> pathParams = parseParameters(path, testDataObject.getJsonArray("pathParams", EMPTY_JSON_ARRAY));
    return new OperationImpl(path, method, operationModel, pathParams);
  }

  @Test
  void testParamFilter() {
    OperationImpl operation = fromTestData("0001_Filter_Path_Parameters", validTestData);

    List<Parameter> params = operation.getParameters();
    assertThat(params).hasSize(1);
    assertThat(params.get(0).isExplode()).isFalse();

    OperationImpl operation2 = fromTestData("0002_Do_Not_Filter_Path_Parameters", validTestData);

    assertThat(operation2.getParameters()).hasSize(2);
  }

  @Test
  void testAdders() {
    String testId = "0000_Test_Getters";
    OperationImpl operation = fromTestData(testId, validTestData);

    Handler<RoutingContext> dummyHandler = RoutingContext::next;
    Handler<RoutingContext> dummyFailureHandler = RoutingContext::next;
    assertThat(dummyHandler).isNotSameInstanceAs(dummyFailureHandler);

    operation.addHandler(dummyHandler);
    assertThat(operation.getHandlers()).containsExactly(dummyHandler);

    operation.addFailureHandler(dummyFailureHandler);
    assertThat(operation.getFailureHandlers()).containsExactly(dummyFailureHandler);
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    OperationImpl operation = fromTestData(testId, validTestData);
    assertThat(operation.getOperationId()).isEqualTo("showPetById");
    assertThat(operation.getOpenAPIPath()).isEqualTo("/pets/{petId}");
    assertThat(operation.getHttpMethod()).isEqualTo(GET);
    assertThat(operation.getTags()).containsExactly("pets", "foo");
    assertThat(operation.getHandlers()).isEmpty();
    assertThat(operation.getFailureHandlers()).isEmpty();

    JsonObject operationModel = validTestData.getJsonObject(testId).getJsonObject("operationModel");
    assertThat(operation.getOperationModel()).isEqualTo(operationModel);

    List<Parameter> params = operation.getParameters();
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getName()).isEqualTo("petId");
    assertThat(params.get(0).getIn()).isEqualTo(PATH);
  }
}
