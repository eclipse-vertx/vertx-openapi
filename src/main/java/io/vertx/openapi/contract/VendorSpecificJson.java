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

package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.GenIgnore;

import java.util.regex.Pattern;

/**
 * Interface to expose specific check about a media type being a vendor specific JSON
 */
@GenIgnore
public interface VendorSpecificJson {

  Pattern VENDOR_SPECIFIC_JSON = Pattern.compile("^[^/]+/vnd\\.[\\w.-]+\\+json$");

  static boolean matches(String type) {
    return type != null && VENDOR_SPECIFIC_JSON.matcher(type).matches();
  }
}
