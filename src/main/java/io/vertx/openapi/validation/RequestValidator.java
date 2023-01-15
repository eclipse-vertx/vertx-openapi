package io.vertx.openapi.validation;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.impl.RequestValidatorImpl;

/**
 * The {@link RequestValidator} requires the {@link RequestParameters parameters} in a specific format to be able to
 * parse and validate them. This is especially true for <i>exploded</i> parameters. The following table shows how the
 * value of a parameter of a request must be stored in a {@link RequestParameters} object. For these examples the key
 * of those values is always <i>color</i>.
 * <p></p>
 * These are the initial values for each type:<br>
 * <ul>
 * <li>primitive (string) -> "blue"</li>
 * <li>array -> ["blue","black","brown"]</li>
 * <li>object -> { "R": 100, "G": 200, "B": 150 }</li>
 * </ul>
 * For cookie parameters {@link RequestParameters#getCookies()}
 * <pre>
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | style  | explode | empty | primitive | array                              | object                  |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | form   | false   |       | blue      | blue,black,brown                   | R,100,G,200,B,150       |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | form   | true    |       | blue      | color=blue&color=black&color=brown | R=100&G=200&B=150       |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * </pre>
 * For header parameters {@link RequestParameters#getHeaders()}
 * <pre>
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | style  | explode | empty | primitive | array                              | object                  |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | simple | false   |       | blue      | blue,black,brown                   | R,100,G,200,B,150       |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * | simple | true    |       | blue      | blue,black,brown                   | R=100,G=200,B=150       |
 * +--------+---------+-------+-----------+------------------------------------+-------------------------+
 * </pre>
 * For path parameters {@link RequestParameters#getPathParameters()}
 * <pre>
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | style  | explode | empty  | primitive   | array                               | object                   |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | simple | false   |        | blue        | blue,black,brown                    | R,100,G,200,B,150        |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | simple | true    |        | blue        | blue,black,brown                    | R=100,G=200,B=150        |
 * +--------+---------+--------+------ ------+-------------------------------------+--------------------------+
 * | label  | false   | .      | .blue       | .blue,black,brown                   | .R,100,G,200,B,150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | label  | true    | .      | .blue       | .blue.black.brown                   | .R=100.G=200.B=150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * | matrix | false   | ;color | ;color=blue | ;color=blue,black,brown             | ;color=R,100,G,200,B,150 |
 * +--------+---------+-------+--------+-------------------------------------------+--------------------------+
 * | matrix | true    | ;color | ;color=blue | ;color=blue;color=black;color=brown | ;R=100;G=200;B=150       |
 * +--------+---------+--------+-------------+-------------------------------------+--------------------------+
 * </pre>
 * For query parameters {@link RequestParameters#getQuery()}
 * <pre>
 * +----------------+---------+-------+------------+-----------------------------------+--------------------------+
 * | style          | explode | empty | primitive | array                              | object                   |
 * +----------------+---------+-------+----------+-------------------------------------+--------------------------+
 * | form           | false   |       | blue      | blue,black,brown                   | R,100,G,200,B,150        |
 * +----------------+---------+-------+-----------+------------------------------------+--------------------------+
 * | form           | true    |       | blue      | color=blue&color=black&color=brown | R=100&G=200&B=150        |
 * +----------------+---------+-------+------ ----+------------------------------------+--------------------------+
 * | spaceDelimited | false   | not yet supported                                                                 /
 * +----------------+---------+-------+------ ----+------------------------------------+--------------------------+
 * | spaceDelimited | true    | not yet supported                                                                 /
 * +----------------+---------+-------+------ ----+------------------------------------+--------------------------+
 * | pipeDelimited  | false   | not yet supported                                                                 /
 * +----------------+---------+-------+------ ----+------------------------------------+--------------------------+
 * | pipeDelimited  | true    | not yet supported                                                                 /
 * +----------------+---------+-------+------ ----+------------------------------------+--------------------------+
 * | spaceDelimited | true    | not yet supported                                                                 /
 * +----------------+---------+-------+------ ----+------------------------------------+--------------------------+
 * </pre>
 */
@VertxGen
public interface RequestValidator {

  /**
   * Create a new {@link RequestValidator}.
   *
   * @param vertx the related Vert.x instance
   * @return an instance of {@link RequestValidator}.
   */
  static RequestValidator create(Vertx vertx, OpenAPIContract contract) {
    return new RequestValidatorImpl(vertx, contract);
  }

  /**
   * Like {@link #validate(RequestParameters, String)}, but the operationId and {@link RequestParameters} are
   * determined from the passed request.
   * <p></p>
   * <b>Note:</b> Determining the operationId is expensive. If possible use {@link #validate(HttpServerRequest, String)}.
   *
   * @param request the request to validate
   * @return A succeeded Future with the parsed and validated request parameters, or a failed Future containing ValidationException.
   */
  Future<RequestParameters> validate(HttpServerRequest request);

  /**
   * Like {@link #validate(RequestParameters, String)}, but {@link RequestParameters} are directly extracted from the passed request.
   *
   * @param request     the request to validate
   * @param operationId the id of the related operation.
   * @return A succeeded Future with the parsed and validated request parameters, or a failed Future containing ValidationException.
   */
  Future<RequestParameters> validate(HttpServerRequest request, String operationId);

  /**
   * Validates the passed request parameters against the operation defined in the related OpenAPI contract.
   *
   * @param params      the request parameters to validate.
   * @param operationId the id of the related operation.
   * @return A succeeded Future with the parsed and validated request parameters, or a failed Future containing ValidationException.
   */
  Future<RequestParameters> validate(RequestParameters params, String operationId);
}
