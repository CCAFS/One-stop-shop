package org.cgiar.ccafs.oss.ingestion.core.stages;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cgiar.ccafs.oss.ingestion.core.Document;

import java.util.Optional;

public interface Stage {
  void initialize(ObjectNode configuration);
  Optional<Document> process(Document document);
  void onStart(String connector);
  void onFinish(String connector);
}
