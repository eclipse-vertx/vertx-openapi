package io.vertx.openapi;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;

import java.util.Optional;

import static io.vertx.json.schema.Draft.DRAFT202012;
import static io.vertx.openapi.RouterBuilderException.createInvalidContract;
import static io.vertx.openapi.RouterBuilderException.createUnsupportedVersion;

public enum OpenAPIVersion {
  V3_1("3.1.0", DRAFT202012);

  private final String schemaVersion;
  private final String schemaFile;
  private final Draft draft;
  private SchemaRepository repository;

  private OpenAPIVersion(String schemaVersion, Draft draft) {
    this.schemaVersion = schemaVersion;
    this.schemaFile = "schemas/OpenAPI" + schemaVersion.replace('.', '_') + ".json";
    this.draft = draft;
  }

  private Future<JsonSchema> loadSchema(Vertx vertx) {
    return vertx.fileSystem().readFile(schemaFile).map(Buffer::toJsonObject).map(JsonSchema::of);
  }

  public Future<OutputUnit> validate(Vertx vertx, SchemaRepository repo, JsonObject contract) {
    return loadSchema(vertx).compose(
      schema -> vertx.executeBlocking(p -> p.complete(repo.validator(schema).validate(contract))));
  }

  public Future<JsonObject> resolve(Vertx vertx, SchemaRepository repo, JsonObject contract) {
    return vertx.executeBlocking(p -> p.complete(repo.resolve(JsonSchema.of(contract))));
  }

  public Future<SchemaRepository> getRepository(Vertx vertx, String baseUri) {
    JsonSchemaOptions opts = new JsonSchemaOptions().setDraft(draft).setBaseUri(baseUri);
    return loadSchema(vertx).compose(openApiSchema -> vertx.executeBlocking(p -> {
      SchemaRepository repo = SchemaRepository.create(opts).preloadMetaSchema(vertx.fileSystem());
      repo.dereference(openApiSchema);
      p.complete(repo);
    }));
  }

  public static OpenAPIVersion fromContract(JsonObject contract) {
    String version = Optional.ofNullable(contract.getString("openapi"))
      .orElseThrow(() -> createInvalidContract("Field \"openapi\" is missing"));

    if (V3_1.schemaVersion.equals(version)) {
      return V3_1;
    } else {
      throw createUnsupportedVersion(version);
    }
  }
}
