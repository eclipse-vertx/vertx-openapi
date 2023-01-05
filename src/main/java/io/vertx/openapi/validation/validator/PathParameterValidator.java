package io.vertx.openapi.validation.validator;

import io.vertx.json.schema.OutputUnit;
import io.vertx.json.schema.SchemaRepository;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Style;
import io.vertx.openapi.validation.RequestParameter;
import io.vertx.openapi.validation.ValidatorException;
import io.vertx.openapi.validation.impl.RequestParameterImpl;
import io.vertx.openapi.validation.validator.transformer.LabelTransformer;
import io.vertx.openapi.validation.validator.transformer.MatrixTransformer;
import io.vertx.openapi.validation.validator.transformer.SimpleTransformer;
import io.vertx.openapi.validation.validator.transformer.ValueTransformer;

import java.util.HashMap;
import java.util.Map;

import static io.vertx.openapi.validation.ValidatorException.createInvalidValue;
import static io.vertx.openapi.validation.ValidatorException.createMissingRequiredParameter;

public class PathParameterValidator {
  private static final Map<Style, ValueTransformer> VALUE_TRANSFORMERS = new HashMap<>(3);

  static {
    VALUE_TRANSFORMERS.put(Style.SIMPLE, new SimpleTransformer());
    VALUE_TRANSFORMERS.put(Style.LABEL, new LabelTransformer());
    VALUE_TRANSFORMERS.put(Style.MATRIX, new MatrixTransformer());
  }

  private final SchemaRepository repository;

  /**
   * Creates a new PathParamValidator.
   *
   * @param repository The repository to create a JsonSchema validator.
   */
  public PathParameterValidator(SchemaRepository repository) {
    this.repository = repository;
  }

  public RequestParameter validate(Parameter parameter, RequestParameter value) throws ValidatorException {
    if (value == null || value.isNull()) {
      if (parameter.isRequired()) {
        throw createMissingRequiredParameter(parameter);
      } else {
        return new RequestParameterImpl(null);
      }
    } else {
      ValueTransformer transformer = VALUE_TRANSFORMERS.get(parameter.getStyle());
      Object transformedValue = transformer.transform(parameter, value.getString());
      OutputUnit result = repository.validator(parameter.getSchema()).validate(transformedValue);
      if (Boolean.TRUE.equals(result.getValid())) {
        return new RequestParameterImpl(transformedValue);
      }
      throw createInvalidValue(parameter, result);
    }
  }
}
