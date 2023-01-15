package io.vertx.openapi;

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
}
