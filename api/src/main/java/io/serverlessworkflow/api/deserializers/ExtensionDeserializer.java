/*
 * Copyright 2020-Present The Serverless Workflow Specification Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.serverlessworkflow.api.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.serverlessworkflow.api.interfaces.Extension;
import io.serverlessworkflow.api.interfaces.WorkflowPropertySource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionDeserializer extends StdDeserializer<Extension> {

  private WorkflowPropertySource context;
  private Map<String, Class<? extends Extension>> extensionsMap = new HashMap<>();
  private static Logger logger = LoggerFactory.getLogger(ExtensionDeserializer.class);

  public ExtensionDeserializer() {
    this(Extension.class);
  }

  public ExtensionDeserializer(Class<?> vc) {
    super(vc);
  }

  public ExtensionDeserializer(WorkflowPropertySource context) {
    this(Extension.class);
    this.context = context;
  }

  public void addExtension(String extensionId, Class<? extends Extension> extensionClass) {
    this.extensionsMap.put(extensionId, extensionClass);
  }

  @Override
  public Extension deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    JsonNode node = jp.getCodec().readTree(jp);

    String extensionId = node.get("extensionid").asText();

    if (context != null) {
      try {
        String result = context.getPropertySource().getProperty(extensionId);

        if (result != null) {
          extensionId = result;
        }
      } catch (Exception e) {
        logger.info("Exception trying to evaluate property: {}", e.getMessage());
      }
    }

    // based on the name return the specific extension impl
    if (extensionsMap != null && extensionsMap.containsKey(extensionId)) {
      return mapper.treeToValue(node, extensionsMap.get(extensionId));
    } else {
      throw new IllegalArgumentException("Extension handler not registered for: " + extensionId);
    }
  }
}
