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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.json.schema.JsonSchemaValidationException;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.MediaType;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.mediatype.ContentAnalyser;
import io.vertx.openapi.validation.ValidationContext;
import io.vertx.openapi.validation.ValidatorException;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.openapi.validation.SchemaValidationException.createInvalidValueBody;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;
import static io.vertx.openapi.validation.ValidatorException.createOperationIdInvalid;

public class BaseValidator {
  protected final Vertx vertx;
  protected final OpenAPIContract contract;

  public BaseValidator(Vertx vertx, OpenAPIContract contract) {
    this.vertx = vertx;
    this.contract = contract;
  }

  protected Future<Operation> getOperation(String operationId) {
    Operation operation = contract.operation(operationId);
    if (operation == null) {
      return failedFuture(createOperationIdInvalid(operationId));
    }
    return succeededFuture(operation);
  }

  protected boolean isSchemaValidationRequired(MediaType mediaType) {
    if (mediaType.getSchema() == null) {
      // content should be treated as binary, because no media model is defined (OpenAPI 3.1)
      return false;
    } else {
      String type = mediaType.getSchema().get("type");
      String format = mediaType.getSchema().get("format");

      // Also a binary string could have length restrictions, therefore we need to preclude further properties.
      boolean noFurtherProperties = mediaType.getSchema().fieldNames().size() == 2;

      if ("string".equalsIgnoreCase(type) && "binary".equalsIgnoreCase(format) && noFurtherProperties) {
        return false;
      }
      return true;
    }
  }

  protected RequestParameterImpl validate(MediaType mediaType, String contentType, Buffer rawContent,
                                          ValidationContext requestOrResponse) {
    ContentAnalyser contentAnalyser = mediaType == null ? null :
      contract.mediaTypes().createContentAnalyser(contentType, rawContent, requestOrResponse);

    if (contentAnalyser == null) {
      throw new ValidatorException("The format of the " + requestOrResponse + " body is not supported",
        UNSUPPORTED_VALUE_FORMAT);
    }

    // Throws an exception if the content is not syntactically correct
    contentAnalyser.checkSyntacticalCorrectness();

    if (isSchemaValidationRequired(mediaType)) {
      Object transformedValue = contentAnalyser.transform();
      OutputUnit result = contract.getSchemaRepository().validator(mediaType.getSchema()).validate(transformedValue);
      try {
        result.checkValidity();
        return new RequestParameterImpl(transformedValue);
      } catch (JsonSchemaValidationException e) {
        throw createInvalidValueBody(result, requestOrResponse, e);
      }
    }

    return new RequestParameterImpl(rawContent);
  }
}
