package org.cgiar.ccafs.oss.ingestion.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.connectors.ConnectorManager;

import java.io.File;

public class IngestionController {
  private static final Logger logger = LogManager.getLogger(IngestionController.class);
  private ConnectorManager connectorManager;

  public IngestionController(String configurationDirectory) {
    File configFile = new File(configurationDirectory, "config.json");
    if (!configFile.exists()) {
      String message = String.format("Can not load config.json file from directory %s", configurationDirectory);
      logger.fatal(message);
      throw new IngestionException(message);
    }
  }
}
