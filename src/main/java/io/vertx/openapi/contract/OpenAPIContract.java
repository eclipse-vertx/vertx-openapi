package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;

import java.util.List;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;

@VertxGen
public interface OpenAPIContract {

  /**
   * @param vertx
   * @param unresolvedContract
   * @return
   */
  static Future<OpenAPIContract> from(Vertx vertx, JsonObject unresolvedContract) {
    if (unresolvedContract == null) {
      return failedFuture(createInvalidContract("Spec must not be null"));
    }

    OpenAPIVersion version = OpenAPIVersion.fromContract(unresolvedContract);
    String baseUri = "app://";

    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<OpenAPIContract> promise = ctx.promise();

    version.getRepository(vertx, baseUri).compose(repository ->
      version.validate(vertx, repository, unresolvedContract).compose(res -> {
        if (Boolean.FALSE.equals(res.getValid())) {
          return failedFuture(createInvalidContract(null, res.toException("")));
        } else {
          return version.resolve(vertx, repository, unresolvedContract);
        }
      }).map(resolvedSpec -> (OpenAPIContract) new OpenAPIContractImpl(resolvedSpec, version))
    ).onComplete(promise);

    return promise.future();
  }

  /**
   * Access to an operation defined in the contract with {@code operationId}.
   *
   * @param operationId the id of the operation.
   * @return the requested operation.
   * @throws IllegalArgumentException if the operation id doesn't exist in the contract.
   */
  @Nullable
  Operation operation(String operationId);

  /**
   * @return all operations defined in the contract.
   */
  List<Operation> operations();

  /**
   * @return all {@link Path Paths} defined in the OpenAPI contract.
   */
  List<Path> gtePaths();

  /**
   * @return the resolved OpenAPI contract as {@link JsonObject}.
   */
  JsonObject getRawContract();

  /**
   * @return the OpenAPI version of the contract.
   */
  OpenAPIVersion getVersion();
}
