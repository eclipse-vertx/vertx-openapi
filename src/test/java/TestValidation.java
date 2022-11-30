import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.openapi.RouterBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class TestValidation {

  @Test
  void test(Vertx vertx, VertxTestContext testContext) {

    JsonObject j = vertx.fileSystem().readFileBlocking("v3.1/petstore.json").toJsonObject();

    RouterBuilder.create(vertx, j).onComplete(testContext.succeedingThenComplete());
  }

  @Test
  void manualLoadAndTest(Vertx vertx) {
    final FileSystem fs = vertx.fileSystem();

    // get a repo ready
    SchemaRepository repo = SchemaRepository
      .create(new JsonSchemaOptions().setDraft(Draft.DRAFT202012).setBaseUri("app12://"))
      .preloadMetaSchema(fs);

    // get a validator for the schema itself
    Validator draft202012 = repo.validator("https://json-schema.org/draft/2020-12/schema");

    // now load the openapi spec to the repo after validating that the fargments are valid
    for (String ref : new String[] {"https://spec.openapis.org/oas/3.1/dialect/base", "https://spec.openapis.org/oas/3.1/meta/base", "https://spec.openapis.org/oas/3.1/schema-base/2022-10-07", "https://spec.openapis.org/oas/3.1/schema/2022-10-07"}) {
      JsonObject raw = new JsonObject(fs.readFileBlocking(ref.substring("https://".length())));
      assertTrue(draft202012.validate(raw).getValid());
      repo.dereference(ref, JsonSchema.of(raw));
    }

    // all fragments ok, get a validator for the user api document
    Validator openapi31 = repo.validator("https://spec.openapis.org/oas/3.1/schema/2022-10-07");

    // load the petstore example
    JsonObject petstore = new JsonObject(fs.readFileBlocking("v3.1/petstore.json"));
    // validate the user api document
    assertTrue(openapi31.validate(petstore).getValid());

    JsonObject dereferencedPetstore = JsonSchema.of(petstore).resolve();

    assertFalse(dereferencedPetstore.encodePrettily().contains("$ref"));
  }
}
