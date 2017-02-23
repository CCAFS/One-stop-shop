package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class PipelineManager {
  private static final Logger logger = LogManager.getLogger(PipelineManager.class);
  private ObjectNode configuration;
  private StageManager stageManager;
  private Map<String, Pipeline> pipelines = new HashMap<>();

  public PipelineManager(ObjectNode configuration, StageManager stageManager) {
    this.configuration = configuration;
    this.stageManager = stageManager;
    initialize();
  }

  private void initialize() {
    logger.debug(String.format("Loading configuration from %s", configuration.toString()));
    Optional<ArrayNode> pipelines = Optional.of((ArrayNode) configuration.get("pipelines"));
    if (pipelines.isPresent()) {
      for (Iterator<JsonNode> it = pipelines.get().elements(); it.hasNext(); ) {
        ObjectNode elem = (ObjectNode) it.next();
        configurePipeline(elem);
      }
    }
    else {
      logger.warn("There are no pipelines configured");
    }
  }

  private void configurePipeline(ObjectNode elem) {
    Optional<JsonNode> pipelineName = Optional.of(elem.get("name"));
    Optional<JsonNode> stages = Optional.of(elem.get("stages"));
    if (pipelineName.isPresent() && stages.isPresent()) {
      Pipeline pipeline;
      pipelines.put(pipelineName.get().asText(), pipeline = new Pipeline());
      if (stages.get().isArray()) {
        ArrayNode arr = (ArrayNode) stages.get();
        for (Iterator<JsonNode> it = arr.elements(); it.hasNext(); ) {
          JsonNode stage = it.next();
          if (stage.isTextual()) {
            if (stageManager.hasStage(stage.asText())) {
              pipeline.addStage(stageManager.getStage(stage.asText()));
            }
            else {
              String msg = String.format("Stage %s referenced in pipeline %s has not been configured", stage.asText(), pipelineName.get().asText());
              fatalError(msg);
            }
          }
          else {
            String msg = String.format("Element %s in stages array for pipeline %s is not a textual node: %s", stage.toString(), pipelineName.get().asText(), elem.toString());
            fatalError(msg);
          }
        }
      }
      else {
        String msg = String.format("Stages node in pipeline %s configuration is not an array: %s", pipelineName.get().asText(), elem.toString());
        fatalError(msg);
      }
    }
    else {
      String msg = "Unable to load pipeline from configuration: " + elem.toString();
      fatalError(msg);
    }
  }

  private void fatalError(String msg) {
    logger.fatal(msg);
    throw new IngestionException(msg);
  }

  public Pipeline getPipeline(String pipelineName) {
    return pipelines.get(pipelineName);
  }
}
