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

package io.vertx.tests.contract.impl;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.vertx.openapi.contract.impl.PathFinder;
import io.vertx.openapi.contract.impl.PathImpl;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class PathFinderTest {

  private static Stream<Arguments> testTestSegments() {
    return Stream.of(
        Arguments.of("/v3/api/user", "/v3/api/user", 6),
        Arguments.of("/v3/api/user", "/{version}/api/user", 3),
        Arguments.of("/v3/api/user", "/{version}/api/users", -1),
        Arguments.of("/v3/api/user/foo", "/{version}/api/user/{username}", 5),
        Arguments.of("/v3/foo", "/{version}/{username}", 0));
  }

  @ParameterizedTest(name = "Segments to test: {index} {0} and {1}")
  @MethodSource("testTestSegments")
  void testTestSegments(String path, String templatePath, int amount) {
    String[] pathSegments = path.substring(1).split("/");
    String[] pathTemplateSegments = templatePath.substring(1).split("/");

    PathFinder pathFinder = new PathFinder(emptyList());
    assertThat(pathFinder.testSegments(pathSegments, pathTemplateSegments)).isEqualTo(amount);
  }

  private PathImpl mockPath(String basePath, String path) {
    PathImpl mockedPath = Mockito.mock(PathImpl.class);
    when(mockedPath.getName()).thenReturn(path);
    when(mockedPath.getAbsolutePath()).thenReturn(basePath + path);
    return mockedPath;
  }

  // to ensure that issue 47 occur
  @RepeatedTest(10)
  void testFindPath() {
    PathImpl v0 = mockPath("", "/v0/api/user");
    PathImpl variableVersion = mockPath("", "/{version}/api/user");
    PathImpl withUsername = mockPath("", "/{version}/api/user/{username}");
    PathImpl withConcreteUser = mockPath("", "/{version}/api/user/hodor");
    PathImpl withConcreteUserAndVersion = mockPath("", "/v3/api/user/hodor");
    PathImpl withConcreteUserAndMockedApi = mockPath("", "/v3/{api}/test/user/foobar");
    PathImpl withConcreteUserAndNotMockedApi = mockPath("", "/v3/api/{test}/user/foobar");
    PathImpl root = mockPath("", "/");
    PathImpl rootWithVersion = mockPath("", "/{version}");

    List<PathImpl> paths =
        ImmutableList.of(v0, variableVersion, withUsername, withConcreteUser, withConcreteUserAndVersion,
            withConcreteUserAndMockedApi, withConcreteUserAndNotMockedApi, root, rootWithVersion);
    PathFinder pathFinder = new PathFinder(paths);

    assertThat(pathFinder.findPath("/v0/api/user")).isEqualTo(v0);
    assertThat(pathFinder.findPath("/v1/api/user")).isEqualTo(variableVersion);
    assertThat(pathFinder.findPath("/v1/api/users")).isNull();

    assertThat(pathFinder.findPath("/v0/api/user/foo")).isEqualTo(withUsername);
    assertThat(pathFinder.findPath("/v1/api/user/foo")).isEqualTo(withUsername);
    assertThat(pathFinder.findPath("/v1/api/user/hodor")).isEqualTo(withConcreteUser);
    assertThat(pathFinder.findPath("/v3/api/user/hodor")).isEqualTo(withConcreteUserAndVersion);
    assertThat(pathFinder.findPath("/v3/api/test/user/foobar")).isEqualTo(withConcreteUserAndNotMockedApi);

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
