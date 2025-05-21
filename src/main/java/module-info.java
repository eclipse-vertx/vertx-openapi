module io.vertx.openapi {

  requires io.netty.codec.http;
  requires io.netty.common;
  requires org.yaml.snakeyaml;
  requires transitive io.vertx.core;
  requires transitive io.vertx.jsonschema;
  requires io.vertx.core.logging;

  requires static io.vertx.codegen.api;
  requires static io.vertx.docgen;

  exports io.vertx.openapi.contract;
  exports io.vertx.openapi.validation;
  exports io.vertx.openapi.validation.transformer;
  exports io.vertx.openapi.mediatype;

  exports io.vertx.openapi.impl to io.vertx.tests;
  exports io.vertx.openapi.validation.impl to io.vertx.tests;
  exports io.vertx.openapi.contract.impl to io.vertx.tests;

  opens io.vertx.openapi.validation.impl to io.vertx.tests;
    exports io.vertx.openapi.mediatype.impl;

}
