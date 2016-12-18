package org.cgiar.ccafs.oss.ingestion.core;

import org.cgiar.ccafs.oss.ingestion.core.stages.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Pipeline {
  private List<Stage> stages = new LinkedList<>();

  public Pipeline addStage(Stage stage) {
    this.stages.add(stage);
    return this;
  }

  public Optional<Document> process(Document document) {
    Document doc = document;
    for (Stage stage : stages) {
      Optional<Document> d = stage.process(doc);
      doc = d.orElse(null);
    }
    return Optional.of(doc);
  }

  public void onStart(String connector) {
    for (Stage stage: stages) {
      stage.onStart(connector);
    }
  }

  public void onStop(String connector) {
    for (Stage stage: stages) {
      stage.onFinish(connector);
    }
  }
}
