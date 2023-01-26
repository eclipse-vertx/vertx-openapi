package examples;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Path;

import java.util.HashMap;
import java.util.Map;

public class ContractExamples {

  public void createContract(Vertx vertx) {
    String pathToContract = ".../.../myContract.json"; // json or yaml
    Future<OpenAPIContract> contract = OpenAPIContract.from(vertx, pathToContract);
  }

  public void createContractAdditionalFiles(Vertx vertx) {
    String pathToContract = ".../.../myContract.json"; // json or yaml
    String pathToComponents = ".../.../myComponents.json"; // json or yaml
    Map<String, String> additionalContractFiles = new HashMap<>();
    additionalContractFiles.put("https://example.com/pet-components",
      pathToComponents);

    Future<OpenAPIContract> contract =
      OpenAPIContract.from(vertx, pathToContract, additionalContractFiles);
  }

  public void pathParameterOperationExample() {
    OpenAPIContract contract = getContract();

    for (Path path : contract.getPaths()) {
      for (Parameter pathParameter : path.getParameters()) {
        // example methods of a OpenAPI parameter object
        pathParameter.isRequired();
        pathParameter.getSchema();
      }
      for (Operation operation : path.getOperations()) {
        // example methods of a OpenAPI operation object
        operation.getOperationId();
        operation.getRequestBody();
        operation.getParameters();
      }
    }
  }

  private OpenAPIContract getContract() {
    return null;
  }
}
