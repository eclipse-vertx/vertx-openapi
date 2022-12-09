package io.vertx.openapi.impl;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.openapi.RouterBuilder;
import io.vertx.openapi.SecurityScheme;
import io.vertx.openapi.objects.Operation;
import io.vertx.openapi.objects.Path;
import io.vertx.openapi.objects.impl.PathImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.vertx.openapi.RouterBuilderException.createInvalidContract;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class RouterBuilderImpl implements RouterBuilder {

  private static final String KEY_PATHS = "paths";

  private static final String PATH_PARAM_PLACEHOLDER_REGEX = "\\{(.*?)\\}";

  private static final UnaryOperator<String> ELIMINATE_PATH_PARAM_PLACEHOLDER =
    path -> path.replaceAll(PATH_PARAM_PLACEHOLDER_REGEX, "{}");

  // VisibleForTesting
  final List<Handler<RoutingContext>> rootHandlers = new ArrayList<>();
  private final Vertx vertx;
  private final List<Path> paths;
  private final List<Handler<RoutingContext>> securityHandlers = new ArrayList<>();

  private final Map<String, Operation> operations;

  public RouterBuilderImpl(JsonObject resolvedSpec, Vertx vertx) {
    this.vertx = vertx;
    List<PathImpl> unsortedPaths = resolvedSpec.getJsonObject(KEY_PATHS, EMPTY_JSON_OBJECT).stream()
      .map(pathEntry -> new PathImpl(pathEntry.getKey(), (JsonObject) pathEntry.getValue())).collect(toList());
    this.paths = unmodifiableList(applyMountOrder(unsortedPaths));
    this.operations = paths.stream().flatMap(path -> path.getOperations().stream()).collect(toMap(
      Operation::getOperationId, operation -> operation));
  }

  /**
   * From <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#paths-object">Paths documentation</a>:
   * <br>
   * Path templating is allowed. When matching URLs, concrete (non-templated) paths would be matched before their
   * templated counterparts. Templated paths with the same hierarchy but different templated names MUST NOT exist as
   * they are identical. In case of ambiguous matching, it's up to the tooling to decide which one to use.
   *
   * @return A List which contains paths without path variables first.
   */
  // VisibleForTesting
  static List<PathImpl> applyMountOrder(List<PathImpl> unsorted) {
    if (unsorted.size() <= 1) {
      return unsorted;
    }

    List<PathImpl> withTemplating = new ArrayList<>();
    List<PathImpl> withoutTemplating = new ArrayList<>();

    for (PathImpl path : unsorted) {
      if (path.getName().contains("{")) {
        withTemplating.add(path);
      } else {
        withoutTemplating.add(path);
      }
    }

    Collections.sort(withTemplating, comparing(p -> ELIMINATE_PATH_PARAM_PLACEHOLDER.apply(p.getName())));
    Collections.sort(withoutTemplating, comparing(p -> ELIMINATE_PATH_PARAM_PLACEHOLDER.apply(p.getName())));

    // Check for Paths with same hierarchy but different templated names
    for (int x = 1; x < withTemplating.size(); x++) {
      String first = withTemplating.get(x - 1).getName();
      String firstWithoutPlaceHolder = ELIMINATE_PATH_PARAM_PLACEHOLDER.apply(first);
      String second = withTemplating.get(x).getName();
      String secondWithoutPlaceholder = ELIMINATE_PATH_PARAM_PLACEHOLDER.apply(second);

      if (firstWithoutPlaceHolder.equals(secondWithoutPlaceholder)) {
        if (first.equals(second)) {
          throw createInvalidContract("Found Path duplicate: " + first);
        } else {
          throw createInvalidContract(
            "Found Paths with same hierarchy but different templated names: " + firstWithoutPlaceHolder);
        }
      }
    }

    withoutTemplating.addAll(withTemplating);

    return withoutTemplating;
  }

  /**
   * @param openAPIPath the path with placeholders in OpenAPI format
   * @return the path with placeholders in vertx-web format
   */
  //VisibleForTesting
  static String toVertxWebPath(String openAPIPath) {
    return openAPIPath.replaceAll(PATH_PARAM_PLACEHOLDER_REGEX, ":$1");
  }

  @Override
  public @Nullable Operation operation(String operationId) {
    return operations.get(operationId);
  }

  @Override
  public List<Operation> operations() {
    return unmodifiableList(new ArrayList<>(operations.values()));
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

    for (Path path : paths) {
      for (Operation operation : path.getOperations()) {
        Route route = router.route(operation.getHttpMethod(), toVertxWebPath(path.getName()));
        operation.getHandlers().forEach(route::handler);
        operation.getFailureHandlers().forEach(route::failureHandler);
      }
    }
    return router;
  }
}
