package io.vertx.openapi.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.RouterBuilder;
import io.vertx.openapi.SecurityScheme;
import io.vertx.openapi.objects.Operation;
import io.vertx.openapi.objects.Path;
import io.vertx.openapi.objects.impl.PathImpl;

import java.util.List;

import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class RouterBuilderImpl implements RouterBuilder {

  private static final String KEY_PATHS = "paths";

  private final JsonObject spec;
  private final SchemaRepository schemaRepository;
  private final Vertx vertx;
  private final List<Path> paths;

  public RouterBuilderImpl(JsonObject resolvedSpec, SchemaRepository schemaRepository, Vertx vertx) {
    this.spec = resolvedSpec;
    this.schemaRepository = schemaRepository;
    this.vertx = vertx;
    this.paths = unmodifiableList(spec.getJsonObject(KEY_PATHS, EMPTY_JSON_OBJECT).stream()
      .map(paths -> new PathImpl(paths.getKey(), (JsonObject) paths.getValue())).collect(toList()));
  }

  /**
   * From <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#parameterObject">Paths documentation</a>:
   * <br>
   * Path templating is allowed. When matching URLs, concrete (non-templated) paths would be matched before their
   * templated counterparts. Templated paths with the same hierarchy but different templated names MUST NOT exist as
   * they are identical. In case of ambiguous matching, it's up to the tooling to decide which one to use.
   *
   * @return A List which contains paths without path variables first.
   */
  private static List<Path> applyMountOrder() {
    return null; // TODO
  }

  @Override public @Nullable Operation operation(String operationId) {
    return null;
  }

  @Override public List<Operation> operations() {
    return null;
  }

  @Override public RouterBuilder rootHandler(Handler<RoutingContext> rootHandler) {
    return null;
  }

  @Override public RouterBuilder securityHandler(String securitySchemeName, AuthenticationHandler handler) {
    return null;
  }

  @Override public SecurityScheme securityHandler(String securitySchemeName) {
    return null;
  }

  @Override public Router createRouter() {
    Router router = Router.router(vertx);
    Route rootRoute = router.route();
    // first mount paths without templating (path variables)
    // then mount variables

    return null;
  }
}
