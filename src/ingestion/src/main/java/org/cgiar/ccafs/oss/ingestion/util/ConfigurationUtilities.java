package org.cgiar.ccafs.oss.ingestion.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cgiar.ccafs.oss.ingestion.core.IngestionException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class ConfigurationUtilities {
  public static int safeInteger(ObjectNode node, String key, int defaultValue) {
    Optional<JsonNode> n = Optional.ofNullable(node.get(key));
    return n.isPresent() && n.get().isInt() ? n.get().asInt() : defaultValue;
  }

  public static String safeString(ObjectNode node, String key, String defaultValue) {
    Optional<JsonNode> n = Optional.ofNullable(node.get(key));
    return n.isPresent() && n.get().isTextual() ? n.get().asText() : defaultValue;
  }

  public static URI buildURI(String uri) {
    try {
      return new URI(uri);
    }
    catch (URISyntaxException e) {
      throw new IngestionException("Syntax error building URI", e);
    }
  }
}
