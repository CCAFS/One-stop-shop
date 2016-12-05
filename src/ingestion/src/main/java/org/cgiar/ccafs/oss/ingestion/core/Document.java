package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigInteger;

/**
 * ===========================================================
 * Created by Eduardo Quiros-Campos - ArkiTechTura Consulting
 * ===========================================================
 **/
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
    ArrayNode scopes = root.putArray(SCOPES_FIELD);
  }

  public ObjectNode addScope(String scopeName) {
    return ((ArrayNode) root.get(SCOPES_FIELD)).addObject();
  }

  public ObjectNode getScope(String scopeName) {
    return root.has(scopeName) ? (ObjectNode) root.get(scopeName) : addScope(scopeName);
  }
}
