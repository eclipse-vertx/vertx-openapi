package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;

import java.util.Arrays;
import java.util.function.Predicate;

@VertxGen
public enum Style {
  MATRIX("matrix"), LABEL("label"), FORM("form"), SIMPLE("simple"), SPACE_DELIMITED(
    "spaceDelimited"), PIPE_DELIMITED("pipeDelimited"), DEEP_OBJECT("deepObject");

  private final String openAPIvalue;

  Style(String openAPIvalue) {
    this.openAPIvalue = openAPIvalue;
  }

  public static Style parse(String style) {
    Predicate<String> eq = Predicate.isEqual(style);
    // Contract validation happened before, so it will find one of these values.
    return style == null ? null : Arrays.stream(Style.values()).filter(l -> eq.test(l.toString())).findFirst().get();
  }

  public static Style defaultByLocation(Location in) {
    switch (in) {
      case COOKIE:
      case QUERY:
        return FORM;
      case PATH:
      case HEADER:
        return SIMPLE;
      default:
        return null;
    }
  }

  @Override
  public String toString() {
    return openAPIvalue;
  }
}
