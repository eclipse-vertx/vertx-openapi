package examples;

import io.vertx.core.Vertx;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.router.RouterBuilder;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ValidatableRequest;

public class ValidationExamples {

  private OpenAPIContract getContract() {
    return null;
  }

  private void createValidator(Vertx vertx, RouterBuilder routerBuilder) {
    OpenAPIContract contract = getContract();
    RequestValidator validator = RequestValidator.create(vertx, contract);

    vertx.createHttpServer().requestHandler(httpServerRequest -> {
      // Operation id must be determined for every request which is inefficient
      validator.validate(httpServerRequest).onSuccess(validatedRequest -> {
        validatedRequest.getBody(); // returns the body
        validatedRequest.getHeaders(); // returns the header
        // ..
        // ..
      });

      // Operation id will be passed to save effort for determining
      validator.validate(httpServerRequest, "yourOperationId")
        .onSuccess(validatedRequest -> {
          // do something
        });
    }).listen(0);
  }

  private ValidatableRequest getValidatableRequest() {
    return null;
  }

  private void validatableRequest(Vertx vertx, RouterBuilder routerBuilder) {
    OpenAPIContract contract = getContract();
    RequestValidator validator = RequestValidator.create(vertx, contract);

    ValidatableRequest request = getValidatableRequest();
    validator.validate(request, "yourOperationId").onSuccess(validatedRequest -> {
      validatedRequest.getBody(); // returns the body
      validatedRequest.getHeaders(); // returns the header
      // ..
      // ..
    });
  }
}
