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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;

class PathFinderTest {

  private static Stream<Arguments> testTestSegments() {
    return Stream.of(
      Arguments.of("/v3/api/user", "/v3/api/user", true),
      Arguments.of("/v3/api/user", "/{version}/api/user", true),
      Arguments.of("/v3/api/user", "/{version}/api/users", false),
      Arguments.of("/v3/api/user/foo", "/{version}/api/user/{username}", true)
    );
  }

  @ParameterizedTest(name = "Segments to test: {index} {0} and {1}")
  @MethodSource("testTestSegments")
  void testTestSegments(String path, String templatePath, boolean equal) {
    String[] pathSegments = path.substring(1).split("/");
    String[] pathTemplateSegments = templatePath.substring(1).split("/");

    PathFinder pathFinder = new PathFinder(emptyList());
    assertThat(pathFinder.testSegments(pathSegments, pathTemplateSegments)).isEqualTo(equal);
  }

  private PathImpl mockPath(String basePath, String path) {
    PathImpl mockedPath = Mockito.mock(PathImpl.class);
    when(mockedPath.getName()).thenReturn(path);
    when(mockedPath.getAbsolutePath()).thenReturn(basePath + path);
    return mockedPath;
  }

  @Test
  void testFindPath() {
    PathImpl v0 = mockPath("", "/v0/api/user");
    PathImpl variableVersion = mockPath("", "/{version}/api/user");
    PathImpl withUsername = mockPath("", "/{version}/api/user/{username}");
    PathImpl root = mockPath("", "/");
    PathImpl rootWithVersion = mockPath("", "/{version}");
    List<PathImpl> paths = ImmutableList.of(v0, variableVersion, withUsername, root, rootWithVersion);
    PathFinder pathFinder = new PathFinder(paths);

    assertThat(pathFinder.findPath("/v0/api/user")).isEqualTo(v0);
    assertThat(pathFinder.findPath("/v1/api/user")).isEqualTo(variableVersion);
    assertThat(pathFinder.findPath("/v1/api/users")).isNull();

    assertThat(pathFinder.findPath("/v0/api/user/foo")).isEqualTo(withUsername);
    assertThat(pathFinder.findPath("/v1/api/user/foo")).isEqualTo(withUsername);

    assertThat(pathFinder.findPath("/v0/api/user/foo/age")).isNull();
    assertThat(pathFinder.findPath("/v1/api/user/foo/age")).isNull();

    assertThat(pathFinder.findPath("/")).isEqualTo(root);
    assertThat(pathFinder.findPath("//")).isNull();

    assertThat(pathFinder.findPath("/v0")).isEqualTo(rootWithVersion);
    assertThat(pathFinder.findPath("/v0/foo")).isNull();
  }

  @Test
  void testFindPathWithBasePath() {
    String basePath = "/base";
    PathImpl v0 = mockPath(basePath, "/v0/api/user");
    PathImpl variableVersion = mockPath(basePath, "/{version}/api/user");
    PathImpl withUsername = mockPath(basePath, "/{version}/api/user/{username}");
    List<PathImpl> paths = ImmutableList.of(v0, variableVersion, withUsername);
    PathFinder pathFinder = new PathFinder(paths);

    assertThat(pathFinder.findPath(basePath + "/v0/api/user")).isEqualTo(v0);
    assertThat(pathFinder.findPath(basePath + "/v1/api/user")).isEqualTo(variableVersion);
    assertThat(pathFinder.findPath(basePath + "/v1/api/users")).isNull();

    assertThat(pathFinder.findPath(basePath + "/v0/api/user/foo")).isEqualTo(withUsername);
    assertThat(pathFinder.findPath(basePath + "/v1/api/user/foo")).isEqualTo(withUsername);

    assertThat(pathFinder.findPath(basePath + "/v0/api/user/foo/age")).isNull();
    assertThat(pathFinder.findPath(basePath + "/v1/api/user/foo/age")).isNull();
  }
}
