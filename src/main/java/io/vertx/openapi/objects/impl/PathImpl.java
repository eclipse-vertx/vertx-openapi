package io.vertx.openapi.objects.impl;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.openapi.RouterBuilderException;
import io.vertx.openapi.objects.Operation;
import io.vertx.openapi.objects.Parameter;
import io.vertx.openapi.objects.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.HttpMethod.OPTIONS;
import static io.vertx.core.http.HttpMethod.PATCH;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static io.vertx.core.http.HttpMethod.TRACE;
import static io.vertx.openapi.Utils.EMPTY_JSON_ARRAY;
import static io.vertx.openapi.objects.impl.ParameterImpl.parseParameters;
import static java.util.Collections.unmodifiableList;

public class PathImpl implements Path {
  private static final String KEY_PARAMETERS = "parameters";

  private static final Map<String, HttpMethod> SUPPORTED_METHODS;

  static {
    SUPPORTED_METHODS = new HashMap<>(8);
    SUPPORTED_METHODS.put("get", GET);
    SUPPORTED_METHODS.put("put", PUT);
    SUPPORTED_METHODS.put("post", POST);
    SUPPORTED_METHODS.put("delete", DELETE);
    SUPPORTED_METHODS.put("options", OPTIONS);
    SUPPORTED_METHODS.put("head", HEAD);
    SUPPORTED_METHODS.put("patch", PATCH);
    SUPPORTED_METHODS.put("trace", TRACE);
  }

  private final String name;
  private final List<Operation> operations;
  private final List<Parameter> parameters;

  public PathImpl(String name, JsonObject pathModel) {
    if (name.contains("*")) {
      throw RouterBuilderException.createInvalidContract("Paths must not have a wildcard (asterisk): " + name);
    }
    this.name = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    this.parameters = unmodifiableList(parseParameters(name, pathModel.getJsonArray(KEY_PARAMETERS, EMPTY_JSON_ARRAY)));

    List<Operation> ops = new ArrayList<>();
    SUPPORTED_METHODS.forEach((methodName, method) -> {
      Optional.ofNullable(pathModel.getJsonObject(methodName))
        .map(operationModel -> new OperationImpl(name, method, operationModel, parameters))
        .ifPresent(ops::add);
    });
    this.operations = unmodifiableList(ops);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Operation> getOperations() {
    return operations;
  }

  @Override
  public List<Parameter> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return name;
  }
}
