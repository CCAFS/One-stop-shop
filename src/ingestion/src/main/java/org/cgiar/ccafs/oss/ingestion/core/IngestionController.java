package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.connectors.ConnectorManager;
import org.cgiar.ccafs.oss.ingestion.util.ConfigurationUtilities;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IngestionController {
  private static final Logger logger = LogManager.getLogger(IngestionController.class);
  private ConnectorManager connectorManager;
  private ExecutorService threadPool;

  private IngestionController(ObjectNode configuration) {
    connectorManager = new ConnectorManager(configuration);
    threadPool = Executors.newFixedThreadPool(ConfigurationUtilities.safeInteger(configuration, "ingestionThreads", 5));
  }

  public static IngestionController fromDirectory(String configurationDirectory) {
    File configFile = new File(configurationDirectory, "config.json");
    if (!configFile.exists()) {
      String message = String.format("Can not load config.json file from directory %s", configurationDirectory);
      logger.fatal(message);
      throw new IngestionException(message);
    }
    try {
      return new IngestionController(loadConfigurationFile(new FileInputStream(configFile)));
    }
    catch (IOException e) {
      throw new IngestionException("Error creating ingestion controller", e);
    }
  }

  public static IngestionController fromStream(InputStream inputStream) {
    return new IngestionController(loadConfigurationFile(inputStream));
  }

  private static ObjectNode loadConfigurationFile(InputStream configuration) {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = null;
    try {
      root = mapper.readTree(configuration);
    }
    catch (IOException e) {
      throw new IngestionException("Error reading JSON tree", e);
    }
    if (!(root instanceof ObjectNode)) {
      throw new IngestionException("Configuration file is malformed. Root object must be JSON Object");
    }
    return (ObjectNode) root;
  }

  public void startCrawl(String connector) {
    CrawlController crawlController = connectorManager.startCrawl(connector);
    threadPool.submit(new IngestionRunnable(crawlController));
  }

  public void stopCrawl(String connector) {
  }

  private class IngestionRunnable implements Runnable {
    private CrawlController crawlController;

    IngestionRunnable(CrawlController crawlController) {
      this.crawlController = crawlController;
    }

    @Override
    public void run() {
      crawlController.join();

    }
  }
}
