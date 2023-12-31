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
   * Results into:
   * <p>
   * [2, (/v1/users, /v1/friends) ]
   */
  private final Map<Integer, List<PathImpl>> segmentsWithoutTemplating = new HashMap<>();

  /**
   * /v1/user/{userid}/name
   * <br>
   * /v1/user/{userid}/age
   * <br>
   * /v1/user/{userid}/friends/{friendId}
   * <p>
   * Results into:
   * <p>
   * [4, [ [Path, (v1, user, {userid}, name)],  [Path, (v1, user, {userid}, age)] ] ]
   * <br>
   * [5, [ [Path, (v1, user, {userid}, friends,{friendId})] ] ]
   */
  private final Map<Integer, Map<Path, String[]>> segmentsWithTemplating = new HashMap<>();

  public PathFinder(List<PathImpl> paths) {
    for (PathImpl path : paths) {
      String[] segments = path.getAbsolutePath().substring(1).split("/");
      if (path.getName().contains("{")) {
        segmentsWithTemplating.computeIfAbsent(segments.length, i -> new HashMap<>()).put(path, segments);
      } else {
        segmentsWithoutTemplating.computeIfAbsent(segments.length, i -> new ArrayList<>()).add(path);
      }
    }
  }

  public Path findPath(String path) {
    String[] segments = path.substring(1).split("/");

    for (PathImpl p : segmentsWithoutTemplating.getOrDefault(segments.length, emptyList())) {
      if (p.getAbsolutePath().equals(path)) {
        return p;
      }
    }

    Path bestMatchedPath = null;
    int currentMatchedSegments = -1;
    for (Entry<Path, String[]> entry : segmentsWithTemplating.getOrDefault(segments.length, emptyMap()).entrySet()) {
      if (testSegments(segments, entry.getValue())) {
        int matchedSegments = getNumSegmentsMatching(segments, entry.getValue());
        if(matchedSegments > currentMatchedSegments) {
          bestMatchedPath = entry.getKey();
          currentMatchedSegments = matchedSegments;
        }
      }
    }

    return bestMatchedPath;
  }

  // VisibleForTesting
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

  private int getNumSegmentsMatching(String[] pathSegments, String[] pathTemplateSegments) {
    int numPerfectMatches = 0;

    for (int i = 0; i < pathTemplateSegments.length; i++) {
      String templateSegment = pathTemplateSegments[i];
      if(templateSegment.equals(pathSegments[i])) {
        numPerfectMatches += pathTemplateSegments.length - i + 1;
      }
    }
    return numPerfectMatches;
  }

}
