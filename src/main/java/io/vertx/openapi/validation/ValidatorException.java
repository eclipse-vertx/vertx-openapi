package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.json.schema.OutputUnit;
import io.vertx.openapi.contract.Parameter;

import java.util.Objects;

import static io.vertx.openapi.validation.ValidatorErrorType.ILLEGAL_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE;
import static io.vertx.openapi.validation.ValidatorErrorType.INVALID_VALUE_FORMAT;
import static io.vertx.openapi.validation.ValidatorErrorType.MISSING_REQUIRED_PARAMETER;
import static io.vertx.openapi.validation.ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT;

public class ValidatorException extends RuntimeException {

  private final ValidatorErrorType type;

  private final OutputUnit outputUnit;

  public ValidatorException(String message, ValidatorErrorType type) {
    this(message, type, null);
  }

  public ValidatorException(String message, ValidatorErrorType type, OutputUnit outputUnit) {
    super(message);
    this.type = type;
    this.outputUnit = outputUnit;
  }

  public static ValidatorException createMissingRequiredParameter(Parameter parameter) {
    String msg = String.format("The related request does not contain the required %s parameter %s",
      parameter.getIn().name().toLowerCase(), parameter.getName());
    return new ValidatorException(msg, MISSING_REQUIRED_PARAMETER);
  }

  public static ValidatorException createInvalidValueFormat(Parameter parameter) {
    String msg = String.format("The formatting of the value of %s parameter %s doesn't match to style %s.",
      parameter.getIn().name().toLowerCase(), parameter.getName(), parameter.getStyle());
    return new ValidatorException(msg, INVALID_VALUE_FORMAT);
  }

  public static ValidatorException createUnsupportedValueFormat(Parameter parameter) {
    String msg =
      String.format("Values in style %s with exploded=%s are not supported for %s parameter %s.", parameter.getStyle(),
        parameter.isExplode(), parameter.getIn().name().toLowerCase(), parameter.getName());
    return new ValidatorException(msg, UNSUPPORTED_VALUE_FORMAT);
  }

  public static ValidatorException createCantDecodeValue(Parameter parameter) {
    String msg = String.format("The value of %s parameter %s can't be decoded.", parameter.getIn().name().toLowerCase(),
      parameter.getName());
    return new ValidatorException(msg, ILLEGAL_VALUE);
  }

  public static ValidatorException createInvalidValue(Parameter parameter, OutputUnit reason) {
    String msg = String.format("The value of %s parameter %s is invalid. Reason: %s",
      parameter.getIn().name().toLowerCase(), parameter.getName(), extractErrorMsg(reason));
    return new ValidatorException(msg, INVALID_VALUE, reason);
  }

  private static String extractErrorMsg(OutputUnit outputUnit) {
    if (outputUnit.getError() != null) {
      return outputUnit.getError();
    }
    return outputUnit.getErrors().stream().map(OutputUnit::getError).filter(Objects::nonNull).findFirst().orElse("n/a");
  }

  public ValidatorErrorType type() {
    return type;
  }

  /**
   * @return the related OutputUnit with the reason in case the Exception is of type {@link ValidatorErrorType#INVALID_VALUE}
   */
  @Nullable
  public OutputUnit getReason() {
    return outputUnit;
  }
}
