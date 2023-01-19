package io.vertx.openapi.router.impl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Path;
import io.vertx.openapi.router.RouterBuilder;
import io.vertx.openapi.router.SecurityScheme;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.impl.RequestValidatorImpl;

import java.util.ArrayList;
import java.util.List;

public class RouterBuilderImpl implements RouterBuilder {
  private static final String PATH_PARAM_PLACEHOLDER_REGEX = "\\{(.*?)}";

  // VisibleForTesting
  final List<Handler<RoutingContext>> rootHandlers = new ArrayList<>();
  private final Vertx vertx;
  private final List<Handler<RoutingContext>> securityHandlers = new ArrayList<>();

  private final OpenAPIContract contract;

  public RouterBuilderImpl(Vertx vertx, OpenAPIContract contract) {
    this.vertx = vertx;
    this.contract = contract;
  }

  /**
   * @param openAPIPath the path with placeholders in OpenAPI format
   * @return the path with placeholders in vertx-web format
   */
  // VisibleForTesting
  static String toVertxWebPath(String openAPIPath) {
    return openAPIPath.replaceAll(PATH_PARAM_PLACEHOLDER_REGEX, ":$1");
  }

  @Override
  public @Nullable Operation operation(String operationId) {
    return contract.operation(operationId);
  }

  @Override
  public List<Operation> operations() {
    return contract.operations();
  }

  @Override
  @Fluent
  public RouterBuilder rootHandler(Handler<RoutingContext> rootHandler) {
    rootHandlers.add(rootHandler);
    return this;
  }

  @Override
  @Fluent
  public RouterBuilder securityHandler(String securitySchemeName, AuthenticationHandler securityHandler) {
    securityHandlers.add(securityHandler);
    return this;
  }

  @Override
  public SecurityScheme securityHandler(String securitySchemeName) {
    return null;
  }

  @Override
  public Router createRouter() {
    Router router = Router.router(vertx);
    RequestValidator validator = new RequestValidatorImpl(vertx, contract);

    for (Path path : contract.getPaths()) {
      for (Operation operation : path.getOperations()) {
        Route route = router.route(operation.getHttpMethod(), toVertxWebPath(path.getName()));
        route.putMetadata(KEY_META_DATA_OPERATION, operation.getOperationId());
        route.handler(rc -> validator.validate(rc.request(), operation.getOperationId()).onSuccess(rp -> {
          rc.put(KEY_META_DATA_VALIDATED_REQUEST, rp);
          rc.next();
        }).onFailure(rc::fail));
        operation.getHandlers().forEach(route::handler);
        operation.getFailureHandlers().forEach(route::failureHandler);
      }
    }
    return router;
  }
}
