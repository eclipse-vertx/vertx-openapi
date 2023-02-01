/*
 * Copyright (c) 2023, Red Hat Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 */
package io.vertx.openapi.impl;

import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.Date;

/**
 * A SafeConstructor that converts timestamps to Instant to comply with Vert.x JsonObject expectations.
 *
 * @author <a href="a href="mailto:pmlopes@gmail.com">Paulo Lopes</a>
 */
public class OpenAPIYamlConstructor extends SafeConstructor {

  public OpenAPIYamlConstructor() {
    super();
    this.yamlConstructors.put(Tag.TIMESTAMP, new ConstructInstantTimestamp());
  }

  private static class ConstructInstantTimestamp extends SafeConstructor.ConstructYamlTimestamp {
    public Object construct(Node node) {
      Date date = (Date) super.construct(node);
      return date.toInstant();
    }
  }
}
