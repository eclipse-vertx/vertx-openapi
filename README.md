# Vert.x OpenAPI

[![Build Status (5.x)](https://github.com/eclipse-vertx/vertx-openapi/actions/workflows/ci-5.x.yml/badge.svg)](https://github.com/eclipse-vertx/vertx-openapi/actions/workflows/ci-5.x.yml)
[![Build Status (4.x)](https://github.com/eclipse-vertx/vertx-openapi/actions/workflows/ci-4.x.yml/badge.svg)](https://github.com/eclipse-vertx/vertx-openapi/actions/workflows/ci-4.x.yml)

Please see the in-source asciidoc documentation or the main documentation on the web-site for a full description
of this component:

* [web-site docs](https://vertx.io/docs/vertx-openapi/java/)
* [in-source docs](src/main/asciidoc/index.adoc)

## Spotless Formatter
Spotless will be executed on the `check` goal during the `validation` phase. If you want to apply the formatter rules automatically run `mvn spotless::apply`

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







