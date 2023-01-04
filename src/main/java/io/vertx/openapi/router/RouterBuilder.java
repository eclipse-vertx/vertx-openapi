package io.vertx.openapi.router;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.router.impl.RouterBuilderImpl;

import java.util.List;

/**
 * Interface to build a Vert.x Web {@link Router} from an OpenAPI 3 contract.
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
  static RouterBuilder create(Vertx vertx, OpenAPIContract contract) {
    return new RouterBuilderImpl(vertx, contract);
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
