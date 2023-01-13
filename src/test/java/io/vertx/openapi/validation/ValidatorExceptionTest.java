package io.vertx.openapi.validation;

import io.vertx.json.schema.JsonSchema;
import io.vertx.openapi.contract.Parameter;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.openapi.MockHelper.mockParameter;
import static io.vertx.openapi.Utils.EMPTY_JSON_OBJECT;
import static io.vertx.openapi.contract.Location.PATH;
import static io.vertx.openapi.contract.Style.LABEL;

class ValidatorExceptionTest {

  private static final Parameter DUMMY_PARAMETER =
    mockParameter("dummy", PATH, LABEL, false, JsonSchema.of(EMPTY_JSON_OBJECT));

  @Test
  void testCreateMissingRequiredParameter() {
    ValidatorException exception = ValidatorException.createMissingRequiredParameter(DUMMY_PARAMETER);
    String expectedMsg = "The related request does not contain the required path parameter dummy";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_REQUIRED_PARAMETER);
  }

  @Test
  void testCreateInvalidValueFormat() {
    ValidatorException exception = ValidatorException.createInvalidValueFormat(DUMMY_PARAMETER);
    String expectedMsg = "The formatting of the value of path parameter dummy doesn't match to style label.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.INVALID_VALUE_FORMAT);
  }

  @Test
  void testCreateUnsupportedValueFormat() {
    ValidatorException exception = ValidatorException.createUnsupportedValueFormat(DUMMY_PARAMETER);
    String expectedMsg = "Values in style label with exploded=false are not supported for path parameter dummy.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.UNSUPPORTED_VALUE_FORMAT);
  }

  @Test
  void testCreateCantDecodeValue() {
    ValidatorException exception = ValidatorException.createCantDecodeValue(DUMMY_PARAMETER);
    String expectedMsg = "The value of path parameter dummy can't be decoded.";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.ILLEGAL_VALUE);
  }

  @Test
  void testCreateOperationIdInvalid() {
    ValidatorException exception = ValidatorException.createOperationIdInvalid("getPets");
    String expectedMsg = "Invalid OperationId: getPets";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_OPERATION);
  }

  @Test
  void testCreateOperationNotFound() {
    ValidatorException exception = ValidatorException.createOperationNotFound(GET, "/my/path");
    String expectedMsg = "No operation found for the request: GET /my/path";
    assertThat(exception).hasMessageThat().isEqualTo(expectedMsg);
    assertThat(exception.type()).isEqualTo(ValidatorErrorType.MISSING_OPERATION);
  }
}
