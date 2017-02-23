package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.jws.soap.SOAPBinding;
import java.math.BigInteger;
import java.util.Optional;


public class Document {
  private static JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);
  private static String SCOPES_FIELD = "scopes";
  public static String CONNECTOR_SCOPE = "connector";

  private ObjectNode root;
  public Document() {
    root = jsonNodeFactory.objectNode();
    setupScopesArray();
  }

  private void setupScopesArray() {
    root.putObject(SCOPES_FIELD);
  }

  public ObjectNode addScope(String scopeName) {
    return ((ObjectNode) root.get(SCOPES_FIELD)).putObject(scopeName);
  }

  public void setScope(String scopeName, JsonNode scope) {
    ((ObjectNode) root.get(SCOPES_FIELD)).set(scopeName, scope);
  }

  public Optional<JsonNode> getScope(String scopeName) {
    if (root.get(SCOPES_FIELD).has(scopeName)) {
      return Optional.of(root.get(SCOPES_FIELD).get(scopeName));
    }
    return Optional.empty();
  }
}
