package io.vertx.openapi.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.Operation;
import io.vertx.openapi.RouterBuilder;
import io.vertx.openapi.SecurityScheme;

import java.util.List;

public class RouterBuilderImpl implements RouterBuilder {

    private final JsonObject spec;

    private final SchemaRepository schemaRepository;

    public RouterBuilderImpl(JsonObject resolvedSpec, SchemaRepository schemaRepository) {
       this.spec = resolvedSpec;
      this.schemaRepository = schemaRepository;
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





      return null;
    }
}
