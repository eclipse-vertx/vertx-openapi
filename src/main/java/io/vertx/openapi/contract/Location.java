package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;

import java.util.Arrays;
import java.util.function.Predicate;

@VertxGen
public enum Location {
  QUERY, HEADER, PATH, COOKIE;

  public static Location parse(String location) {
    Predicate<String> eq = Predicate.isEqual(location);
    // Contract validation happened before, so it will find one of these values.
    return location == null ?
      null :
      Arrays.stream(Location.values()).filter(l -> eq.test(l.toString())).findFirst().get();
  }

  @Override
  public String toString() {
    return super.name().toLowerCase();
  }
}
