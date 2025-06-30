/*
 * Copyright 2023 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.openapi.contract;

import io.vertx.codegen.annotations.VertxGen;
import java.util.List;
import java.util.Set;

/**
 * A Security requirement is an object that contains the names of the security schemes that apply to the operation.
 * Each name has a list of scopes that apply to the operation.
 *
 * @author Paulo Lopes
 */
@VertxGen
public interface SecurityRequirement extends OpenAPIObject {

  /**
   * How many requirements are present
   * @return size
   */
  int size();

  /**
   * Return the name at a given index
   * @return name
   */
  Set<String> getNames();

  /**
   * Return the scopes for a given name
   * @return name
   */
  List<String> getScopes(String name);

  boolean isEmpty();
}
