package io.vertx.openapi;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.openapi.impl.RouterBuilderImpl;
import io.vertx.openapi.objects.Operation;

import java.util.List;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.RouterBuilderException.createInvalidContract;

/**
 * Interface to build a Vert.x Web {@link Router} from an OpenAPI 3 contract. <br/>
 * To add a handler, use {@link RouterBuilder#operation(String)} (String, Handler)}<br/>
 * Usage example:
 * <pre>
 * {@code
 * RouterBuilder.create(vertx, "src/resources/spec.yaml", asyncResult -> {
 *  if (!asyncResult.succeeded()) {
 *     // IO failure or spec invalid
 *  } else {
 *     RouterBuilder routerBuilder = asyncResult.result();
 *     RouterBuilder.operation("operation_id").handler(routingContext -> {
 *        // Do something
 *     }, routingContext -> {
 *        // Do something with failure handler
 *     });
 *     Router router = routerBuilder.createRouter();
 *  }
 * });
 * }
 * </pre>
 * <br/>
 * Handlers are loaded in this order:<br/>
 *  <ol>
 *   <li>Custom global handlers configurable with {@link this#rootHandler(Handler)}</li>
 *   <li>Global security handlers defined in upper spec level</li>
 *   <li>Operation specific security handlers</li>
 *   <li>Generated validation handler</li>
 *   <li>User handlers or "Not implemented" handler</li>
 * </ol>
 *
 * @author Francesco Guardiani @slinkydeveloper
 */
@VertxGen
public interface RouterBuilder {
  String KEY_META_DATA_OPERATION = "openApiOperationId";

  /**
   * Create a new {@link RouterBuilder}.
   *
   * @param vertx the related Vert.x instance
   * @return Future completed with success when specification is loaded and valid
   */
  static Future<RouterBuilder> create(Vertx vertx, JsonObject spec) {
    if (spec == null) {
      throw RouterBuilderException.createInvalidContract("Spec must not be null");
    }

    OpenAPIVersion version = OpenAPIVersion.fromContract(spec);
    String baseUri = "app://";

    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<RouterBuilder> promise = ctx.promise();

    version.getRepository(vertx, baseUri).compose(repository ->
      version.validate(vertx, repository, spec).compose(res -> {
        if (Boolean.FALSE.equals(res.getValid())) {
          return failedFuture(createInvalidContract(null, res.toException("")));
        } else {
          return version.resolve(vertx, repository, spec);
        }
      }).map(resolvedSpec -> (RouterBuilder) new RouterBuilderImpl(resolvedSpec, vertx))
    ).onComplete(promise);

    return promise.future();
  }

  /**
   * Access to an operation defined in the contract with {@code operationId}
   *
   * @param operationId the id of the operation
   * @return the requested operation
   * @throws IllegalArgumentException if the operation id doesn't exist in the contract
   */
  @Nullable
  Operation operation(String operationId);

  /**
   * @return all operations defined in the contract
   */
  List<Operation> operations();

  /**
   * Add global handler to be applied prior to {@link Router} being generated. <br/>
   *
   * @param rootHandler the root handler to add
   * @return self
   */
  @Fluent
  RouterBuilder rootHandler(Handler<RoutingContext> rootHandler);

  /**
   * Mount to paths that have to follow a security schema a security handler. This method will not perform any
   * validation weather or not the given {@code securitySchemeName} is present in the OpenAPI document.
   * <p>
   * For must use cases the method {@link #securityHandler(String)} should be used.
   *
   * @param securitySchemeName the components security scheme id
   * @param handler            the authentication handler
   * @return self
   */
  @Fluent
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  RouterBuilder securityHandler(String securitySchemeName, AuthenticationHandler handler);

  /**
   * Creates a new security scheme for the required {@link AuthenticationHandler}.
   *
   * @return a security scheme.
   */
  SecurityScheme securityHandler(String securitySchemeName);

  /**
   * Construct a new router based on the related OpenAPI contract.
   *
   * @return a Router based on the related OpenAPI contract.
   */
  Router createRouter();
}
