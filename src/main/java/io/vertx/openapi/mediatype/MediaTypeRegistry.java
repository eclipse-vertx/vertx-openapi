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

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.openapi.mediatype.impl.DefaultMediaTypeRegistry;
import java.util.List;

/**
 * The MediaTypeRegistry contains all supported MediaTypes and Validators for the mediatypes. New MediaTypes can be registered
 * by providing new MediaTypeRegistrations.
 */
@VertxGen
public interface MediaTypeRegistry {
  /**
   * Creates a default registry with application/json, application/multipart and text/plain mediatypes registered.
   *
   * @return A registry with default options.
   */
  static MediaTypeRegistry createDefault() {
    return new DefaultMediaTypeRegistry()
        .register(MediaTypeRegistration.APPLICATION_JSON)
        .register(MediaTypeRegistration.MULTIPART_FORM_DATA)
        .register(MediaTypeRegistration.APPLICATION_OCTET_STREAM)
        .register(MediaTypeRegistration.TEXT_PLAIN)
        .register(MediaTypeRegistration.VENDOR_SPECIFIC_JSON);
  }

  /**
   * Creates an empty registry.
   *
   * @return An empty registry.
   */
  static MediaTypeRegistry createEmpty() {
    return new DefaultMediaTypeRegistry();
  }

  /**
   * Registers a new MediaTypeHandler
   *
   * @param registration The mediatype registration.
   * @return This registry for a fluent interface.
   */
  MediaTypeRegistry register(MediaTypeRegistration registration);

  /**
   * Checks if the provided media type is supported by the registration
   *
   * @param type The raw mediatype string
   * @return true if it supported, false otherwise
   */
  default boolean isSupported(String type) {
    return get(type) != null;
  }

  /**
   * @return A list of all supported types.
   */
  List<String> supportedTypes();

  /**
   * Finds the registration for the provided media type.
   * @param mediaType The media type to find the registration for.
   * @return The registration if found, null otherwise.
   */
  MediaTypeRegistration get(String mediaType);
}
