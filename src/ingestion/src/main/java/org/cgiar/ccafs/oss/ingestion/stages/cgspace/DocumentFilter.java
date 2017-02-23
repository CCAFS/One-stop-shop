package org.cgiar.ccafs.oss.ingestion.stages.cgspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cgiar.ccafs.oss.ingestion.core.Document;
import org.cgiar.ccafs.oss.ingestion.core.stages.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class DocumentFilter implements Stage {
  private List<Integer> allowedCommunities = new ArrayList<>();
  String metadataField;
  String metadataValue;

  @Override
  public void initialize(ObjectNode configuration) {
    fillAllowedCommunitiesList(configuration);
    metadataField = configuration.path("metadataField").asText();
    metadataValue = configuration.path("metadataValue").asText();
  }

  private void fillAllowedCommunitiesList(ObjectNode configuration) {
    JsonNode node = configuration.path("allowedCommunities");
    if (node.isArray()) {
      for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
        JsonNode elementNode = it.next();
        if (elementNode.isInt()) {
          allowedCommunities.add(elementNode.intValue());
        }
      }
    }
  }

  @Override
  public Optional<Document> process(Document document) {
    if (isAllowedCommunity(document) || hasValidMetadataValue(document)) {
      return Optional.of(document);
    }
    return Optional.empty();
  }

  private boolean hasValidMetadataValue(Document document) {
    Optional<JsonNode> cgspaceNode = document.getScope("CGSpace");
    if (cgspaceNode.isPresent() && cgspaceNode.get().isArray()) {
      for (Iterator<JsonNode> it = cgspaceNode.get().elements(); it.hasNext(); ) {
        JsonNode meta = it.next();
        if (meta.path("key").asText().equalsIgnoreCase(metadataField) &&
                meta.path("value").asText().equalsIgnoreCase(metadataValue)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isAllowedCommunity(Document document) {
    Optional<JsonNode> metadataNode = document.getScope("Metadata");
    if (metadataNode.isPresent() && metadataNode.get().has("parentCommunityList")) {
      JsonNode node = metadataNode.get().path("parentCommunityList");
      for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
        JsonNode communityNode = it.next();
        if (communityNode.path("id").isInt()) {
          return allowedCommunities.contains(communityNode.path("id").asInt());
        }
      }
    }
    return false;
  }

  @Override
  public void onStart(String connector) {

  }

  @Override
  public void onFinish(String connector) {

  }
}
