package io.vertx.openapi.validation.impl;

import io.vertx.openapi.validation.RequestParameter;

import java.util.Objects;

public class RequestParameterImpl implements RequestParameter {

  private Object value;

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
