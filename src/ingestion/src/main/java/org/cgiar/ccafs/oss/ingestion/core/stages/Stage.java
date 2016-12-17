package org.cgiar.ccafs.oss.ingestion.core.stages;

import org.cgiar.ccafs.oss.ingestion.core.Document;

import java.util.Optional;

public interface Stage {
  Optional<Document> process(Document document);
}
