package io.vertx.openapi.validation.impl;

import io.vertx.openapi.contract.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

class PathFinder {

  /**
   * /v1/users
   * <br>
   * /v1/friends
   * <p>
   * Resultus into:
   * <p>
   * [2, (/v1/users, /v1/friends) ]
   */
  private final Map<Integer, List<Path>> segmentsWithoutTemplating = new HashMap<>();

  /**
   * /v1/user/{userid}/name
   * <br>
   * /v1/user/{userid}/age
   * <br>
   * /v1/user/{userid}/friends/{friendId}
   * <p>
   * Resultus into:
   * <p>
   * [4, [ [Path, (v1, user, {userid}, name)],  [Path, (v1, user, {userid}, age)] ] ]
   * <br>
   * [5, [ [Path, (v1, user, {userid}, friends,{friendId})] ] ]
   */
  private final Map<Integer, Map<Path, String[]>> segmentsWithTemplating = new HashMap<>();

  public PathFinder(List<Path> paths) {
    for (Path path : paths) {
      String[] segments = path.getName().substring(1).split("/");
      if (path.getName().contains("{")) {
        segmentsWithTemplating.computeIfAbsent(segments.length, i -> new HashMap<>()).put(path, segments);
      } else {
        segmentsWithoutTemplating.computeIfAbsent(segments.length, i -> new ArrayList<>()).add(path);
      }
    }
  }

  public Path findPath(String path) {
    String[] segments = path.substring(1).split("/");

    for (Path p : segmentsWithoutTemplating.getOrDefault(segments.length, emptyList())) {
      if (p.getName().equals(path)) {
        return p;
      }
    }

    for (Entry<Path, String[]> entry : segmentsWithTemplating.getOrDefault(segments.length, emptyMap()).entrySet()) {
      if (testSegments(segments, entry.getValue())) {
        return entry.getKey();
      }
    }

    return null;
  }

  //VisibleForTesting
  boolean testSegments(String[] pathSegments, String[] pathTemplateSegments) {
    for (int i = 0; i < pathTemplateSegments.length; i++) {
      String templateSegment = pathTemplateSegments[i];
      if (templateSegment.contains("{") || templateSegment.equals(pathSegments[i])) {
        // valid segment
      } else {
        return false;
      }
    }
    return true;
  }
}
