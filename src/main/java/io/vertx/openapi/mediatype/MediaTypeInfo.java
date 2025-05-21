package io.vertx.openapi.mediatype;

import io.vertx.codegen.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a media type string and provides simple access to the type, the suffix and the parameters.
 */
public class MediaTypeInfo {

  private final String type;
  private final String subtype;
  private final String suffix;
  private final Map<String, String> parameters;

  public MediaTypeInfo(String type, String subtype, @Nullable String suffix, Map<String, String> parameters) {
    this.type = type;
    this.subtype = subtype;
    this.suffix = suffix;
    this.parameters = parameters;
  }

  public static MediaTypeInfo of(String mediaType) {
    var type = new StringBuilder();
    var subtype = new StringBuilder();
    var suffix = new StringBuilder();

    var parameters = new LinkedHashMap<String, String>();
    var paramKey = new StringBuilder();
    var paramValue = new StringBuilder();

    var mode = 0; // 0: type, 1: subtype, 2: suffix, 3: parameter key, 4: parameter value
    for (int i = 0; i < mediaType.length(); i++) {
      var c = mediaType.charAt(i);
      if (c == '/')
        mode = 1;
      else if (c == '+')
        mode = 2;
      else if (c == ';') {
        mode = 3;
        if (paramKey.length() > 0) {
          parameters.put(paramKey.toString().trim(), paramValue.toString().trim());
          paramKey = new StringBuilder();
          paramValue = new StringBuilder();
        }
      } else if (c == '=')
        mode = 4;
      else
        switch (mode) {
          case 0:
            type.append(c);
            break;
          case 1:
            subtype.append(c);
            break;
          case 2:
            suffix.append(c);
            break;
          case 3:
            paramKey.append(c);
            break;
          case 4:
            paramValue.append(c);
            break;
          default:
            throw new IllegalStateException("Should not happen");
        }
    }
    if (paramKey.length() > 0) {
      parameters.put(paramKey.toString().trim(), paramValue.toString().trim());
    }
    return new MediaTypeInfo(
        type.toString(),
        subtype.toString(),
        suffix.length() == 0 ? null : suffix.toString().trim(),
        parameters);
  }

  public String type() {
    return type;
  }

  public String subtype() {
    return subtype;
  }

  /**
   * Get the type including suffix if available.
   *
   * @return type including suffix if available
   */
  public String fullType() {
    var sb = new StringBuilder(type).append("/").append(subtype);
    suffix().ifPresent(s -> sb.append("+").append(s));
    return sb.toString();
  }

  public Map<String, String> parameters() {
    return parameters;
  }

  public Optional<String> suffix() {
    return Optional.ofNullable(suffix);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass())
      return false;
    MediaTypeInfo mediaType = (MediaTypeInfo) o;
    return Objects.equals(type, mediaType.type) && Objects.equals(suffix, mediaType.suffix)
        && Objects.equals(parameters, mediaType.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, suffix, parameters);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder(fullType());
    if (!parameters.isEmpty()) {
      for (var e : parameters.entrySet()) {
        sb.append("; ").append(e.getKey()).append("=").append(e.getValue());
      }
    }
    return sb.toString();
  }

  /**
   * Checks if the other mediatype is compatible to this mediatype. The other mediatype is compatible when it is equal
   * or more specific than this mediatype, e.g. application/vnd.example+json is more specific than application/vnd.example
   * and thus compatible to it, whereas the other way around they are not compatible.
   *
   * @param other The other mediatype
   * @return true if it is compatible, false otherwise
   */
  public boolean doesInclude(MediaTypeInfo other) {
    if (this.type.equals(other.type)) {
      if (this.subtype.equals("*") || this.subtype.equals(other.subtype)) {
        if (this.suffix == null || Objects.equals(this.suffix, other.suffix)) {
          for (var e : this.parameters.entrySet()) {
            if (!(other.parameters.containsKey(e.getKey())
                && Objects.equals(e.getValue(), other.parameters.get(e.getKey())))) {
              // does not include the same parameter, so we fail
              return false;
            }
          }
          // every check was okay so far. They are compatible
          return true;
        }
      }
    }
    return false;
  }
}
