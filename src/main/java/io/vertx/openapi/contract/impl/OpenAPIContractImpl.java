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

package io.vertx.openapi.contract.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchema;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.*;
import io.vertx.openapi.mediatype.MediaTypeRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.vertx.openapi.contract.OpenAPIContractException.createInvalidContract;
import static io.vertx.openapi.contract.OpenAPIContractException.createUnsupportedFeature;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.impl.Utils.EMPTY_JSON_OBJECT;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

public class OpenAPIContractImpl implements OpenAPIContract {
  private static final String KEY_SERVERS = "servers";
  private static final String KEY_PATHS = "paths";
  private static final String KEY_SECURITY = "security";
  private static final String PATH_PARAM_PLACEHOLDER_REGEX = "\\{(.*?)}";
  private static final UnaryOperator<String> ELIMINATE_PATH_PARAM_PLACEHOLDER =
    path -> path.replaceAll(PATH_PARAM_PLACEHOLDER_REGEX, "{}");

  private final List<Server> servers;

  private final List<Path> paths;

  private final Map<String, Operation> operations;

  private final OpenAPIVersion version;

  private final JsonObject rawContract;

  private final SchemaRepository schemaRepository;

  private final PathFinder pathFinder;
  private final List<SecurityRequirement> securityRequirements;

  private final Map<String, SecurityScheme> securitySchemes;

  private final MediaTypeRegistry mediaTypes;


  // VisibleForTesting
  final String basePath;

  public OpenAPIContractImpl(JsonObject resolvedSpec, OpenAPIVersion version, SchemaRepository schemaRepository, MediaTypeRegistry mediaTypes) {
    this.rawContract = resolvedSpec;
    this.version = version;
    this.schemaRepository = schemaRepository;
    this.mediaTypes = mediaTypes;

    servers = resolvedSpec
      .getJsonArray(KEY_SERVERS, EMPTY_JSON_ARRAY)
      .stream()
      .map(JsonObject.class::cast)
      .map(ServerImpl::new).collect(toUnmodifiableList());

    this.securityRequirements = resolvedSpec
      .getJsonArray(KEY_SECURITY, EMPTY_JSON_ARRAY)
      .stream()
      .map(JsonObject.class::cast)
      .map(SecurityRequirementImpl::new).collect(toUnmodifiableList());

    if (servers.stream().collect(groupingBy(Server::getBasePath)).size() > 1) {
      throw createUnsupportedFeature("Different base paths in server urls");
    } else {
      this.basePath = servers.isEmpty() ? "" : servers.get(0).getBasePath();
    }
    List<PathImpl> unsortedPaths = resolvedSpec
      .getJsonObject(KEY_PATHS, EMPTY_JSON_OBJECT)
      .stream()
      .filter(JsonSchema.EXCLUDE_ANNOTATION_ENTRIES)
      .map(pathEntry -> new PathImpl(basePath, pathEntry.getKey(), (JsonObject) pathEntry.getValue(),
        securityRequirements))
      .collect(toList());

    List<PathImpl> sortedPaths = applyMountOrder(unsortedPaths);
    this.paths = unmodifiableList(sortedPaths);
    this.operations = paths.stream().flatMap(path -> path.getOperations().stream()).collect(toMap(
      Operation::getOperationId, operation -> operation));
    // It is important that PathFinder gets the ordered Paths
    this.pathFinder = new PathFinder(sortedPaths);

    this.securitySchemes =
      resolvedSpec
        .getJsonObject("components", EMPTY_JSON_OBJECT)
        .getJsonObject("securitySchemes", EMPTY_JSON_OBJECT)
        .stream()
        .filter(JsonSchema.EXCLUDE_ANNOTATION_ENTRIES)
        .collect(toMap(Map.Entry::getKey, value -> new SecuritySchemeImpl((JsonObject) value.getValue())));
  }

  /**
   * From
   * <a href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#paths-object">Paths documentation</a>:
   * <br>
   * Path templating is allowed. When matching URLs, concrete (non-templated) paths would be matched before their
   * templated counterparts. Templated paths with the same hierarchy but different templated names MUST NOT exist as
   * they are identical. In case of ambiguous matching, it's up to the tooling to decide which one to use.
   *
   * @return A List which contains paths without path variables first.
   */
  // VisibleForTesting
  public static List<PathImpl> applyMountOrder(List<PathImpl> unsorted) {
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

    withTemplating.sort(comparing(p -> ELIMINATE_PATH_PARAM_PLACEHOLDER.apply(p.getName())));
    withoutTemplating.sort(comparing(p -> ELIMINATE_PATH_PARAM_PLACEHOLDER.apply(p.getName())));

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

  public String basePath() {
    return basePath;
  }

  @Override
  public @Nullable Operation operation(String operationId) {
    return operations.get(operationId);
  }

  @Override
  public List<Operation> operations() {
    return List.copyOf(operations.values());
  }

  @Override
  public List<Path> getPaths() {
    return paths;
  }

  @Override
  public JsonObject getRawContract() {
    return rawContract;
  }

  @Override
  public OpenAPIVersion getVersion() {
    return version;
  }

  @Override
  public SchemaRepository getSchemaRepository() {
    return schemaRepository;
  }

  @Override
  public List<Server> getServers() {
    return servers;
  }

  @Override
  public Path findPath(String urlPath) {
    return pathFinder.findPath(urlPath);
  }

  @Override
  public Operation findOperation(String urlPath, HttpMethod method) {
    Path pathObject = findPath(urlPath);
    if (pathObject != null) {
      for (Operation op : pathObject.getOperations()) {
        if (op.getHttpMethod().equals(method)) {
          return op;
        }
      }
    }
    return null;
  }

  @Override
  public List<SecurityRequirement> getSecurityRequirements() {
    return securityRequirements;
  }

  @Override
  public SecurityScheme securityScheme(String name) {
    return securitySchemes.get(name);
  }

  @Override
  public MediaTypeRegistry mediaTypes() { return mediaTypes; }
}
