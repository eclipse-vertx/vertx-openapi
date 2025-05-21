package io.vertx.openapi.mediatype;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A predicate that
 */
public interface MediaTypePredicate extends Predicate<MediaTypeInfo> {

  /**
   * This method is intended for reporting of supported media types in the system.
   *
   * @return The list of supported types.
   */
  List<String> supportedTypes();

  /**
   * Factory for a predicate that checks if the provided mediatype is compatible to any of the types.
   *
   * @param types The types to accept
   * @return The predicate that checks if the mediatype is compatible to one of the types.
   */
  static MediaTypePredicate ofCompatibleTypes(String... types) {
    var list = Arrays.stream(types).map(MediaTypeInfo::of).collect(Collectors.toList());
    return new MediaTypePredicate() {
      @Override
      public List<String> supportedTypes() {
        return list.stream().map(Object::toString).collect(Collectors.toList());
      }

      @Override
      public boolean test(MediaTypeInfo s) {
        return list.stream().anyMatch(x -> x.doesInclude(s));
      }
    };
  }

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
