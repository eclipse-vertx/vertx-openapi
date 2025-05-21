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

package io.vertx.openapi.mediatype.impl;

import io.vertx.openapi.mediatype.MediaTypeRegistration;
import io.vertx.openapi.mediatype.MediaTypeRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation for MediaTypeRegistry
 */
public class DefaultMediaTypeRegistry implements MediaTypeRegistry {
  private final List<MediaTypeRegistration> registrations = new ArrayList<>();

  @Override
  public DefaultMediaTypeRegistry register(MediaTypeRegistration registration) {
    this.registrations.add(registration);
    return this;
  }

  @Override
  public List<String> supportedTypes() {
    return registrations.stream().flatMap(x -> x.supportedTypes().stream()).collect(Collectors.toList());
  }

  @Override
  public MediaTypeRegistration get(String mediaType) {
    return registrations.stream().filter(x -> x.canHandle(mediaType)).findFirst().orElse(null);
  }
}
