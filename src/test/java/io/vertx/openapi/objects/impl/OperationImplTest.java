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
import java.util.Arrays;
import java.util.List;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.objects.Location.PATH;
import static io.vertx.openapi.objects.impl.ParameterImpl.parseParameters;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

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
    assertEquals(1, params.size());
    assertFalse(params.get(0).isExplode());

    OperationImpl operation2 = fromTestData("0002_Do_Not_Filter_Path_Parameters", validTestData);

    List<Parameter> params2 = operation2.getParameters();
    assertEquals(2, params2.size());
  }

  @Test
  void testAdders() {
    String testId = "0000_Test_Getters";
    OperationImpl operation = fromTestData(testId, validTestData);

    Handler<RoutingContext> dummyHandler = RoutingContext::next;
    Handler<RoutingContext> dummyFailureHandler = RoutingContext::next;
    assertNotSame(dummyHandler, dummyFailureHandler);

    operation.addHandler(dummyHandler);
    operation.addFailureHandler(dummyFailureHandler);

    assertEquals(1, operation.getHandlers().size());
    assertSame(dummyHandler, operation.getHandlers().get(0));
    assertEquals(1, operation.getFailureHandlers().size());
    assertSame(dummyFailureHandler, operation.getFailureHandlers().get(0));
  }

  @Test
  void testGetters() {
    String testId = "0000_Test_Getters";
    OperationImpl operation = fromTestData(testId, validTestData);
    assertEquals("showPetById", operation.getOperationId());
    assertEquals("/pets/{petId}", operation.getOpenAPIPath());
    assertEquals(GET, operation.getHttpMethod());
    assertIterableEquals(Arrays.asList("pets", "foo"), operation.getTags());
    assertIterableEquals(emptyList(), operation.getHandlers());
    assertIterableEquals(emptyList(), operation.getFailureHandlers());
    JsonObject operationModel = validTestData.getJsonObject(testId).getJsonObject("operationModel");
    assertEquals(operationModel, operation.getOperationModel());

    List<Parameter> params = operation.getParameters();
    assertEquals(1, params.size());
    assertEquals("petId", params.get(0).getName());
    assertEquals(PATH, params.get(0).getIn());
  }
}
