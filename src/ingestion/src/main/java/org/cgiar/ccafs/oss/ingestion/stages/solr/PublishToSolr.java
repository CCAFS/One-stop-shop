package org.cgiar.ccafs.oss.ingestion.stages.solr;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cgiar.ccafs.oss.ingestion.core.Document;
import org.cgiar.ccafs.oss.ingestion.core.stages.Stage;

import java.util.Optional;

public class PublishToSolr implements Stage {
  public PublishToSolr() {

  }
  @Override
  public void initialize(ObjectNode configuration) {

  }

  @Override
  public Optional<Document> process(Document document) {
    return null;
  }

  @Override
  public void onStart(String connector) {

  }

  @Override
  public void onFinish(String connector) {

  }
}
