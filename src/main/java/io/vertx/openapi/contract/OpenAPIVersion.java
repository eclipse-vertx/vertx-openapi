package io.vertx.openapi.contract;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.vertx.json.schema.Draft.DRAFT202012;
import static io.vertx.json.schema.Draft.DRAFT4;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedVersion;

public enum OpenAPIVersion {
  V3_0("3.0.", DRAFT4, "https://spec.openapis.org/oas/3.0/schema/2021-09-28"),
  V3_1("3.1.", DRAFT202012,
    "https://spec.openapis.org/oas/3.1/schema/2022-10-07",
    "https://spec.openapis.org/oas/3.1/dialect/base",
    "https://spec.openapis.org/oas/3.1/meta/base",
    "https://spec.openapis.org/oas/3.1/schema-base/2022-10-07"
  );

  // VisibleForTesting
  final List<String> schemaFiles;
  private final String schemaVersion;
  private final Draft draft;
  private final String mainSchemaFile;

  OpenAPIVersion(String schemaVersion, Draft draft, String mainSchemaFile, String... additionalSchemaFiles) {
    this.schemaVersion = schemaVersion;
    this.draft = draft;
    this.mainSchemaFile = mainSchemaFile;
    this.schemaFiles = new ArrayList<>(Arrays.asList(additionalSchemaFiles));
    schemaFiles.add(mainSchemaFile);
  }

  public static OpenAPIVersion fromContract(JsonObject contract) {
    String version = Optional.ofNullable(contract).map(spec -> spec.getString("openapi"))
      .orElseThrow(() -> createInvalidContract("Field \"openapi\" is missing"));

    if (version.startsWith(V3_0.schemaVersion)) {
      return V3_0;
    } else if (version.startsWith(V3_1.schemaVersion)) {
      return V3_1;
    } else {
      throw createUnsupportedVersion(version);
    }
  }

  public Future<OutputUnit> validate(Vertx vertx, SchemaRepository repo, JsonObject contract) {
    return vertx.executeBlocking(p -> p.complete(repo.validator(mainSchemaFile).validate(contract)));
  }

  public Future<JsonObject> resolve(Vertx vertx, SchemaRepository repo, JsonObject contract) {
    return vertx.executeBlocking(p -> p.complete(repo.resolve(JsonSchema.of(contract))));
  }

  public Future<SchemaRepository> getRepository(Vertx vertx, String baseUri) {
    JsonSchemaOptions opts = new JsonSchemaOptions().setDraft(draft).setBaseUri(baseUri);
    return vertx.executeBlocking(p -> {
      SchemaRepository repo = SchemaRepository.create(opts).preloadMetaSchema(vertx.fileSystem());
      for (String ref : schemaFiles) {
        JsonObject raw = new JsonObject(vertx.fileSystem().readFileBlocking(ref.substring("https://".length())));
        repo.dereference(ref, JsonSchema.of(raw));
      }
      p.complete(repo);
    });
  }
}
