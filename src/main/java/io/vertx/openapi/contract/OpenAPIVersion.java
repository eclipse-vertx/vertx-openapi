/*
 * Copyright (c) 2023, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.openapi.contract;

import static io.vertx.json.schema.Draft.DRAFT202012;
import static io.vertx.json.schema.Draft.DRAFT4;
import static io.vertx.json.schema.OutputFormat.Basic;
import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedVersion;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonFormatValidator;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.impl.OpenAPIFormatValidator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum OpenAPIVersion {
  V3_0("3.0.", DRAFT4, "https://spec.openapis.org/oas/3.0/schema/2021-09-28",
      new OpenAPIFormatValidator()),
  V3_1("3.1.", DRAFT202012,
      "https://spec.openapis.org/oas/3.1/schema/2022-10-07",
      new OpenAPIFormatValidator(),
      "https://spec.openapis.org/oas/3.1/dialect/base",
      "https://spec.openapis.org/oas/3.1/meta/base",
      "https://spec.openapis.org/oas/3.1/schema-base/2022-10-07");

  // VisibleForTesting
  final List<String> schemaFiles;
  private final String schemaVersion;
  private final Draft draft;
  private final String mainSchemaFile;
  private final JsonFormatValidator formatValidator;

  OpenAPIVersion(String schemaVersion, Draft draft, String mainSchemaFile, JsonFormatValidator formatValidator,
      String... additionalSchemaFiles) {
    this.schemaVersion = schemaVersion;
    this.draft = draft;
    this.mainSchemaFile = mainSchemaFile;
    this.schemaFiles = new ArrayList<>(Arrays.asList(additionalSchemaFiles));
    this.formatValidator = formatValidator;
    schemaFiles.add(mainSchemaFile);
  }

  public List<String> schemaFiles() {
    return schemaFiles;
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

  public Future<OutputUnit> validateContract(Vertx vertx, SchemaRepository repo, JsonObject contract) {
    return vertx.executeBlocking(() -> repo.validator(mainSchemaFile).validate(contract));
  }

  /**
   * Validates additional contract files against the openapi schema. If validations fails, try to validate against the
   * json schema specifications only.
   *
   * @param vertx                   The related Vert.x instance.
   * @param repo                    The SchemaRepository to do the validations with.
   * @param file                    The additional json contract to validate.
   */
  public Future<Void> validateAdditionalContractFile(Vertx vertx, SchemaRepository repo, JsonObject file) {
    return vertx.executeBlocking(() -> repo.validator(draft.getIdentifier()).validate(file))
        .compose(this::checkOutputUnit)
        .mapEmpty();
  }

  private Future<Void> checkOutputUnit(OutputUnit ou) {
    try {
      ou.checkValidity();
      return Future.succeededFuture();
    } catch (JsonSchemaValidationException e) {
      return Future.failedFuture(e);
    }
  }

  public Future<JsonObject> resolve(Vertx vertx, SchemaRepository repo, JsonObject contract) {
    return vertx.executeBlocking(() -> {
      JsonSchema schema = JsonSchema.of(contract);
      repo.dereference(schema);
      return repo.resolve(contract);
    });
  }

  public Future<SchemaRepository> getRepository(Vertx vertx, String baseUri) {
    JsonSchemaOptions opts = new JsonSchemaOptions().setDraft(draft).setBaseUri(baseUri).setOutputFormat(Basic);
    return vertx.executeBlocking(() -> {
      SchemaRepository repo = SchemaRepository.create(opts, formatValidator).preloadMetaSchema(vertx.fileSystem());
      for (String ref : schemaFiles) {
        JsonObject raw = new JsonObject(vertx.fileSystem().readFileBlocking(ref.substring("https://".length())));
        repo.dereference(ref, JsonSchema.of(raw));
      }
      return repo;
    });
  }
}
