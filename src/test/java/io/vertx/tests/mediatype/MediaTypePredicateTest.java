/*
 * Copyright (c) 2025, SAP SE
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.tests.mediatype;

import static com.google.common.truth.Truth.assertThat;

import io.vertx.openapi.mediatype.MediaTypeInfo;
import io.vertx.openapi.mediatype.MediaTypePredicate;
import org.junit.jupiter.api.Test;

class MediaTypePredicateTest {

  @Test
  void testOfExactTypesMatches() {
    MediaTypePredicate predicate = MediaTypePredicate.ofExactTypes("application/json", "text/plain");
    assertThat(predicate.test(MediaTypeInfo.of("application/json"))).isTrue();
    assertThat(predicate.test(MediaTypeInfo.of("text/plain"))).isTrue();
    assertThat(predicate.test(MediaTypeInfo.of("image/png"))).isFalse();
    assertThat(predicate.supportedTypes()).containsExactly("application/json", "text/plain").inOrder();
  }

  @Test
  void testOfRegexpMatches() {
    MediaTypePredicate predicate = MediaTypePredicate.ofRegexp("application/.*");
    assertThat(predicate.test(MediaTypeInfo.of("application/json"))).isTrue();
    assertThat(predicate.test(MediaTypeInfo.of("application/xml"))).isTrue();
    assertThat(predicate.test(MediaTypeInfo.of("text/plain"))).isFalse();
    assertThat(predicate.supportedTypes()).containsExactly("application/.*");
  }
}
