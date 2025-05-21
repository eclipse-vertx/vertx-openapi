/*
 * Copyright (c) 2025, Lukas Jelonek
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */

package io.vertx.openapi.mediatype;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public interface MediaTypePredicate extends Predicate<MediaTypeInfo> {

  /**
   * This method is intended for reporting of supported media types in the system.
   *
   * @return The list of supported types.
   */
  List<String> supportedTypes();

  /**
   * Factory for a predicate that accepts a list of types. Checks if the mediatype is equal to one of the types
   * provided. Only checks the type/subtype+suffix. Ignores the attributes.
   *
   * @param types The types to accept
   * @return The predicate that checks if the mediatype is part of the provided list.
   */
  static MediaTypePredicate ofExactTypes(String... types) {
    var list = List.of(types);
    return new MediaTypePredicate() {
      @Override
      public List<String> supportedTypes() {
        return list;
      }

      @Override
      public boolean test(MediaTypeInfo s) {
        return list.stream().anyMatch(x -> x.equals(s.fullType()));
      }
    };
  }

  /**
   * Factory for a predicate that accepts types based on a regular expression. Only checks the type/subtype+suffix.
   * Ignores the attributes.
   *
   * @param regexp The regular expression
   * @return A predicate that checks if the mediatype matches the regular expression.
   */
  static MediaTypePredicate ofRegexp(String regexp) {
    var pattern = Pattern.compile(regexp);

    return new MediaTypePredicate() {
      @Override
      public List<String> supportedTypes() {
        return List.of(regexp);
      }

      @Override
      public boolean test(MediaTypeInfo s) {
        return pattern.matcher(s.fullType()).matches();
      }
    };
  }
}
