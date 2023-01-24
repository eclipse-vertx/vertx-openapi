package io.vertx.openapi;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourceHelper {

  public static final Path TEST_RESOURCE_PATH = Paths.get("src", "test", "resources");

  private ResourceHelper() {

  }

  public static Path getRelatedTestResourcePath(Class<?> relatedClass) {
    Path related = Paths.get(relatedClass.getPackage().getName().replace(".", "/"));
    return TEST_RESOURCE_PATH.resolve(related);
  }

  public static JsonObject loadJson(Vertx vertx, Path path) {
    return vertx.fileSystem().readFileBlocking(path.toString()).toJsonObject();
  }
}
