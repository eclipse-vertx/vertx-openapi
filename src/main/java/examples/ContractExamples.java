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

package examples;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.contract.Parameter;
import io.vertx.openapi.contract.Path;
import io.vertx.openapi.mediatype.ContentAnalyserFactory;
import io.vertx.openapi.mediatype.MediaTypeRegistration;
import io.vertx.openapi.mediatype.MediaTypePredicate;
import io.vertx.openapi.mediatype.MediaTypeRegistry;
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

  public void createContractWithCustomMediaTypes(Vertx vertx) {
    String pathToContract = ".../.../myContract.json"; // json or yaml
    String pathToComponents = ".../.../myComponents.json"; // json or yaml

    Future<OpenAPIContract> contract =
      OpenAPIContract.builder(vertx)
        .setContractPath(pathToContract)
        .setAdditionalContractPartPaths(Map.of(
          "https://example.com/pet-components", pathToComponents))
        .mediaTypeRegistry(
          MediaTypeRegistry.createDefault()
            .register(
              MediaTypeRegistration.create(
                MediaTypePredicate.ofExactTypes("text/my-custom-type+json"),
                ContentAnalyserFactory.json()))
        )
        .build();
  }
}
