package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.openapi.contract.impl.OpenAPIContractImpl;
import io.vertx.openapi.impl.Utils;
import io.vertx.openapi.mediatype.MediaTypeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;

@GenIgnore
public class OpenAPIContractBuilder {

  public static class OpenAPIContractBuilderException extends RuntimeException {
    public OpenAPIContractBuilderException(String message) {
      super(message);
    }
  }

  private final Vertx vertx;
  private String contractFile;
  private JsonObject contract;
  private final Map<String, String> additionalContentFiles = new HashMap<>();
  private final Map<String, JsonObject> additionalContent = new HashMap<>();
  private MediaTypeRegistry registry;

  public OpenAPIContractBuilder(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Sets the path to the contract file. Either provide the path to the contract or the parsed contract,
   * not both {@link #contract(JsonObject)}.
   *
   * @param contractPath The path to the contract file
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder contract(String contractPath) {
    if (this.contract != null)
      throw new OpenAPIContractBuilderException("A parsed contract was already set. Only set a parsed contract or a path to a contract file, not both.");
    this.contractFile = contractPath;
    return this;
  }

  /**
   * Sets the contract. Either provide the contract or the path to the contract,
   * not both {@link #contract(String)}.
   *
   * @param contract The parsed contract
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder contract(JsonObject contract) {
    if (this.contractFile != null)
      throw new OpenAPIContractBuilderException("A contract file was already set. Only set a parsed contract or a path to a contract file, not both.");
    this.contract = contract;
    return this;
  }

  /**
   * Adds an additional contract that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced contracts.
   *
   * @param key  The unique key for the contract.
   * @param path The path to the contract file.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder addAdditionalContent(String key, String path) {
    checkDuplicateKeys(key);
    additionalContentFiles.put(key, path);
    return this;
  }

  public OpenAPIContractBuilder addAdditionalContentFiles(Map<String, String> otherContractFiles) {
    for (var e : otherContractFiles.entrySet()) {
      addAdditionalContent(e.getKey(), e.getValue());
    }
    return this;
  }


  /**
   * Adds an additional contract that is referenced by the main contract. This method can be
   * called multiple times to add multiple referenced contracts.
   *
   * @param key     The unique key for the contract.
   * @param content The parsed contract.
   * @return The builder, for a fluent interface
   */
  public OpenAPIContractBuilder addAdditionalContent(String key, JsonObject content) {
    checkDuplicateKeys(key);
    additionalContent.put(key, content);
    return this;
  }

  public OpenAPIContractBuilder addAdditionalContent(Map<String, JsonObject> otherContracts) {
    for (var e : otherContracts.entrySet()) {
      addAdditionalContent(e.getKey(), e.getValue());
    }
    return this;
  }

  public OpenAPIContractBuilder mediaTypeRegistry(MediaTypeRegistry registry) {
    this.registry = registry;
    return this;
  }

  private void checkDuplicateKeys(String key) {
    if (additionalContentFiles.containsKey(key) || additionalContent.containsKey(key)) {
      throw new OpenAPIContractBuilderException(String.format("The key '%s' has been added twice.", key));
    }
  }

  /**
   * Builds the contract.
   *
   * @return The contract.
   */
  public Future<OpenAPIContract> build() {
    if (contractFile == null && contract == null) {
      return Future.failedFuture(new OpenAPIContractBuilderException("Neither a contract file or a contract is set. One of them must be set."));
    }
    if (this.registry == null) this.registry = MediaTypeRegistry.createDefault();

    Future<JsonObject> readContract = contractFile == null
      ? Future.succeededFuture(contract)
      : Utils.readYamlOrJson(vertx, contractFile);

    var resolvedContracts = Future
      .succeededFuture(additionalContent)
      .compose(x -> readContractFiles()
        .map(r -> {
          var all = new HashMap<>(x);
          all.putAll(r);
          return all;
        }));

    return Future.all(readContract, resolvedContracts)
      .compose(x -> {
        JsonObject contract = x.resultAt(0);
        Map<String, JsonObject> other = x.resultAt(1);
        return from(contract, other);
      });
  }

  private Future<OpenAPIContract> from(JsonObject unresolvedContract,
                                       Map<String, JsonObject> additionalContractFiles) {
    if (unresolvedContract == null) {
      return failedFuture(createInvalidContract("Spec must not be null"));
    }

    OpenAPIVersion version = OpenAPIVersion.fromContract(unresolvedContract);
    String baseUri = "app://";

    ContextInternal ctx = (ContextInternal) vertx.getOrCreateContext();
    Promise<OpenAPIContract> promise = ctx.promise();

    version.getRepository(vertx, baseUri)
      .compose(repository -> {
        List<Future<?>> validationFutures = new ArrayList<>(additionalContractFiles.size());
        for (String ref : additionalContractFiles.keySet()) {
          // Todo: As soon a more modern Java version is used the validate part could be extracted in a private static
          //  method and reused below.
          JsonObject file = additionalContractFiles.get(ref);
          Future<?> validationFuture = version.validateAdditionalContractFile(vertx, repository, file)
            .compose(v -> vertx.executeBlocking(() -> repository.dereference(ref, JsonSchema.of(ref, file))));

          validationFutures.add(validationFuture);
        }
        return Future.all(validationFutures).map(repository);
      }).compose(repository ->
        version.validateContract(vertx, repository, unresolvedContract).compose(res -> {
            try {
              res.checkValidity();
              return version.resolve(vertx, repository, unresolvedContract);
            } catch (JsonSchemaValidationException | UnsupportedOperationException e) {
              return failedFuture(createInvalidContract(null, e));
            }
          })
          .map(resolvedSpec -> new OpenAPIContractImpl(resolvedSpec, version, repository, registry))
      ).recover(e -> {
        //Convert any non-openapi exceptions into an OpenAPIContractException
        if (e instanceof OpenAPIContractException) {
          return failedFuture(e);
        }

        return failedFuture(createInvalidContract("Found issue in specification for reference: " + e.getMessage(), e));
      }).onComplete(promise);

    return promise.future();
  }

  private Future<Map<String, JsonObject>> readContractFiles() {
    if (additionalContentFiles.isEmpty()) return Future.succeededFuture(Map.of());

    var read = new HashMap<String, JsonObject>();
    return Future.all(additionalContentFiles.entrySet().stream()
        .map(e -> Utils.readYamlOrJson(vertx, e.getValue())
          .map(c -> read.put(e.getKey(), c)))
        .collect(Collectors.toList()))
      .map(ign -> read);
  }

}
