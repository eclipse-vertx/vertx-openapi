# Vert.x OpenAPI

[![CI](https://github.com/eclipse-vertx/vertx-openapi/actions/workflows/ci.yml/badge.svg)](https://github.com/eclipse-vertx/vertx-openapi/actions/workflows/ci.yml)

Please see the in-source asciidoc documentation or the main documentation on the web-site for a full description
of this component:

* [web-site docs](https://vertx.io/docs/vertx-web-openapi/java/)
* [in-source docs](src/main/asciidoc/index.adoc)

## TODOs

### LabelTransformer

How to handle type `number` when style `Label` and exploded is `true`? Because all values are seperated by a '.' (dot), and this char is also used as floating point.

### FormTransformer

How does Cookie Parameter Parser behave when explode is true? see https://swagger.io/docs/specification/serialization/

### Missing Transformer

Due to wrong or insufficient documentation the following transformers are postponed:

- PipeDelimitedTransformer
- SpaceDelimitedTransformer
- DeepObjectTransformer







