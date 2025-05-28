package io.vertx.openapi.mediatype;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a media type string and provides simple access to the type, the suffix and the parameters.
 */
public class MediaType {

    private final String type;
    private final Optional<String> suffix;
    private final Optional<String> parameters;

    public MediaType(String type, Optional<String> suffix, Optional<String> parameters) {
        this.type = type;
        this.suffix = suffix;
        this.parameters = parameters;
    }

    public static MediaType of(String mediaType) {
        var type = new StringBuilder();
        var suffix = new StringBuilder();
        var parameters = new StringBuilder();

        var mode = 0; // 0: type, 1: suffix, 2: parameters
        for (int i = 0; i < mediaType.length(); i++) {
            var c = mediaType.charAt(i);
            if (c == '+') mode = 1;
            else if (c == ';') mode = 2;
            else
                switch (mode) {
                    case 0:
                        type.append(c);
                        break;
                    case 1:
                        suffix.append(c);
                        break;
                    case 2:
                        parameters.append(c);
                        break;
                    default:
                        throw new IllegalStateException("Should not happen");
                }
        }
        return new MediaType(
                type.toString(),
                suffix.length() == 0 ? Optional.empty() : Optional.of(suffix.toString().trim()),
                parameters.length() == 0 ? Optional.empty() : Optional.of(parameters.toString().trim()));
    }

    public String type() {
        return type;
    }

    /**
     * Get the type including suffix if available
     * @return type including suffix if available
     */
    public  String fullType() {
        var sb = new StringBuilder(type);
        suffix.ifPresent(s -> sb.append("+").append(s));
        return sb.toString();
    }

    public Optional<String> parameters() {
        return parameters;
    }

    public Optional<String> suffix() {
        return suffix;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MediaType mediaType = (MediaType) o;
        return Objects.equals(type, mediaType.type) && Objects.equals(suffix, mediaType.suffix) && Objects.equals(parameters, mediaType.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, suffix, parameters);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(type);
        suffix.ifPresent(s -> sb.append("+").append(s));
        parameters.ifPresent(s -> sb.append("; ").append(s));
        return sb.toString();
    }
}
