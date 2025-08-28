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

package io.vertx.openapi.validation;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static java.util.stream.Collectors.joining;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import io.vertx.openapi.validation.impl.ValidatableRequestImpl;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RequestUtils {
  private static final RequestParameter EMPTY = new RequestParameterImpl(null);
  private static final Function<Collection<String>, String> GET_FIRST_VALUE =
      values -> values.stream().findFirst().orElse(null);

  private RequestUtils() {

  }

  /**
   * Extracts and transforms the parameters and the body of an incoming request into a {@link ValidatableRequest format}
   * that can be validated by the {@link io.vertx.openapi.validation.RequestValidator}.
   *
   * @param request   the incoming request.
   * @param operation the operation of the related request.
   * @return A {@link Future} holding the ValidatableRequest.
   */
  public static Future<ValidatableRequest> extract(HttpServerRequest request, Operation operation) {
    return extract(request, operation, request::body);
  }

  /**
   * Like {@link #extract(HttpServerRequest, Operation)}, but offers to pass a supplier fpr the body. This is
   * helpful in case that the request has already been read.
   *
   * @param request      the incoming request.
   * @param operation    the operation of the related request.
   * @param bodySupplier the body supplier which can help in case that the request has already been read.
   * @return A {@link Future} holding the ValidatableRequest.
   */
  public static Future<ValidatableRequest> extract(HttpServerRequest request, Operation operation,
      Supplier<Future<Buffer>> bodySupplier) {
    Map<String, RequestParameter> cookies = new HashMap<>();
    Map<String, RequestParameter> headers = new HashMap<>();
    Map<String, RequestParameter> pathParams = new HashMap<>();
    Map<String, RequestParameter> query = new HashMap<>();

    for (Parameter param : operation.getParameters()) {
      switch (param.getIn()) {
        case COOKIE:
          cookies.put(param.getName(), extractCookie(request, param));
          break;
        case HEADER:
          headers.put(param.getName(), extractHeaders(request, param));
          break;
        case PATH:
          int segment = findPathSegment(operation.getAbsoluteOpenAPIPath(), param.getName());
          pathParams.put(param.getName(), extractPathParameters(param, request, segment));
          break;
        case QUERY:
          query.put(param.getName(), extractQuery(request, param));
      }
    }

    if (operation.getRequestBody() == null) {
      return Future.succeededFuture(new ValidatableRequestImpl(cookies, headers, pathParams, query));
    }

    String contentType = request.headers().get(HttpHeaders.CONTENT_TYPE);
    try {
      return bodySupplier.get().map(buffer -> {
        RequestParameter body = new RequestParameterImpl(buffer);
        return new ValidatableRequestImpl(cookies, headers, pathParams, query, body, contentType);
      });
    } catch (RuntimeException e) {
      return Future.failedFuture(e);
    }
  }

  private static RequestParameter extractCookie(HttpServerRequest request, Parameter parameter) {
    Collection<String> cookies =
        request.cookies(parameter.getName()).stream().map(Cookie::getValue).map(c -> urlDecodeIfRequired(parameter, c))
            .collect(Collectors.toList());
    return joinFormValues(cookies, parameter, () -> {
      String explodedObject =
          request.cookies().stream().map(c -> c.getName() + "=" + urlDecodeIfRequired(parameter, c.getValue()))
              .collect(joining("&"));
      return new RequestParameterImpl(explodedObject);
    });
  }

  private static RequestParameter extractHeaders(HttpServerRequest request, Parameter parameter) {
    String headerValue = request.getHeader(parameter.getName());
    return new RequestParameterImpl(urlDecodeIfRequired(parameter, headerValue));
  }

  private static RequestParameter extractPathParameters(Parameter param, HttpServerRequest request, int segment) {
    String[] pathSegments = request.path().substring(1).split("/");
    if (pathSegments.length < segment) {
      return EMPTY;
    }
    return new RequestParameterImpl(decodeUrl(pathSegments[segment - 1]));
  }

  /**
   * It seems that query parameters are always decoded, so we MUST NOT decode them again.
   */
  private static RequestParameter extractQuery(HttpServerRequest request, Parameter parameter) {
    Collection<String> queryParams = request.params().getAll(parameter.getName());
    return joinFormValues(queryParams, parameter, () -> {
      String decodedQuery =
          request.params().entries().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
              .collect(joining("&"));
      return new RequestParameterImpl(decodedQuery);
    });
  }

  private static RequestParameter joinFormValues(Collection<String> formValues, Parameter parameter,
      Supplier<RequestParameter> explodedObjectSupplier) {
    if (formValues.isEmpty()) {
      return EMPTY;
    }

    switch (parameter.getSchemaType()) {
      case OBJECT:
        if (parameter.isExplode()) {
          return explodedObjectSupplier.get();
        } else {
          return new RequestParameterImpl(GET_FIRST_VALUE.apply(formValues));
        }
      case ARRAY:
        if (parameter.isExplode()) {
          String explodedString =
              formValues.stream().map(fv -> parameter.getName() + "=" + fv).collect(joining("&"));
          return new RequestParameterImpl(explodedString);
        } else {
          return new RequestParameterImpl(GET_FIRST_VALUE.apply(formValues));
        }
      default:
        return new RequestParameterImpl(GET_FIRST_VALUE.apply(formValues));
    }
  }

  // VisibleForTesting
  public static int findPathSegment(String templatePath, String parameterName) {
    int idx = templatePath.indexOf("{" + parameterName + "}");
    return (int) templatePath.subSequence(0, idx).chars().filter(c -> c == '/').count();
  }

  static String urlDecodeIfRequired(Parameter param, String value) {
    boolean requiresDecoding =
        Boolean.TRUE.equals(param.getExtensions().getOrDefault(Parameter.EXTENSION_URLDECODE, false));
    if (requiresDecoding) {
      return decodeUrl(value);
    }
    return value;
  }

  private static String decodeUrl(String encoded) {
    try {
      return encoded == null ? null : URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new ValidatorException("Can't decode URL value: " + encoded, ILLEGAL_VALUE, e);
    }
  }
}
