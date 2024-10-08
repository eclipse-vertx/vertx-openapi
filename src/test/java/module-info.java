open module io.vertx.tests {
  requires com.google.common;
  requires io.netty.codec.http;
  requires io.vertx.core;
  requires io.vertx.jsonschema;
  requires io.vertx.openapi;
  requires io.vertx.testing.junit5;
  requires io.netty.common; // io.netty.handler.codec.http.APPLICATION_JSON - todo remove that and have it declared in Vert.x HttpHeaders
  requires junit;
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;
  requires org.mockito;
  requires truth;

}
