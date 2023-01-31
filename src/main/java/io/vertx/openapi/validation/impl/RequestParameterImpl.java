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

package io.vertx.openapi.validation.impl;

import io.vertx.openapi.validation.RequestParameter;

import java.util.Objects;

public class RequestParameterImpl implements RequestParameter {

  private final Object value;

  public RequestParameterImpl(Object value) {
    this.value = value;
  }

  @Override
  public Object get() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof RequestParameterImpl))
      return false;
    RequestParameterImpl that = (RequestParameterImpl) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
