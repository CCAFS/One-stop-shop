package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.stages.Stage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class StageManager {
  public static final Logger logger = LogManager.getLogger(StageManager.class);
  private Map<String, Stage> stages = new HashMap<>();
  private ObjectNode configuration;

  public StageManager(ObjectNode configuration) {
    logger.debug("Creating StageManager");
    this.configuration = configuration;
    initialize();
  }

  private void initialize() {
    logger.debug(String.format("Loading configuration from %s", configuration.toString()));
    Optional<ArrayNode> stages = Optional.of((ArrayNode) configuration.get("stages"));
    if (stages.isPresent()) {
      for (Iterator<JsonNode> it = stages.get().elements(); it.hasNext(); ) {
        ObjectNode elem = (ObjectNode) it.next();
        loadStage(elem);
      }
    }
    else {
      logger.warn("There are no stages configured");
    }
  }

  private void loadStage(ObjectNode elem) {
    Optional<JsonNode> stageName = Optional.of(elem.get("name"));
    Optional<JsonNode> stageClass = Optional.of(elem.get("class"));
    if (stageName.isPresent() && stageClass.isPresent()) {
      try {
        Stage stage;
        stages.put(stageName.get().asText(), stage = instantiateStage(stageClass.get().asText()));
        stage.initialize((ObjectNode) elem.get("configuration"));
      }
      catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
        String msg = "Error loading stage";
        logger.fatal(msg, e);
        throw new IngestionException(msg, e);
      }
    }
    else {
      String message = "Unable to load stage from configuration: " + elem.toString();
      logger.fatal(message);
      throw new IngestionException(message);
    }
  }

  private Stage instantiateStage(String clazz) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    return (Stage) StageManager.class.getClassLoader().loadClass(clazz).newInstance();
  }

  public Stage getStage(String stageName) {
    return stages.get(stageName);
  }

  public boolean hasStage(String stageName) {
    return stages.containsKey(stageName);
  }
}
