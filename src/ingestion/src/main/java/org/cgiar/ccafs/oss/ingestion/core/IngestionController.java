package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.connectors.Connector;
import org.cgiar.ccafs.oss.ingestion.util.ConfigurationUtilities;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.*;

public class IngestionController {
  private static final Logger logger = LogManager.getLogger(IngestionController.class);
  private ConnectorManager connectorManager;
  private PipelineManager pipelineManager;
  private ExecutorService ingestionThreadPool;
  private ExecutorService crawlThreadPool;
  private ObjectNode configuration;
  private int ingestionThreads;

  private IngestionController(ObjectNode configuration) {
    this.configuration = configuration;
    initialize();
  }

  private void initialize() {
    connectorManager = new ConnectorManager(configuration);
    StageManager stageManager = new StageManager(configuration);
    pipelineManager = new PipelineManager(configuration, stageManager);
    ingestionThreads = ConfigurationUtilities.safeInteger(configuration, "ingestionThreads", 5);
    crawlThreadPool = Executors.newSingleThreadExecutor();
  }

  public static IngestionController fromFile(File configFile) {
    if (!configFile.exists()) {
      String message = String.format("File '%s' to configure the ingestion process does not exist", configFile.getAbsolutePath());
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

  public static IngestionController fromDirectory(String configurationDirectory) {
    File configFile = new File(configurationDirectory, "config.json");
    return fromFile(configFile);
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

  public ObjectNode ingestionStatus(String connector) {
    return connectorManager.crawlStatus(connector);
  }

  public void startCrawl(String connector, int limit) {
    if (connectorManager.canStartCrawl(connector)) {
      crawlThreadPool.submit(() -> {
        ingestionThreadPool = Executors.newFixedThreadPool(ingestionThreads);
        startPipeline(connector);
        CrawlController crawlController = connectorManager.startCrawl(connector, limit);
        for (int t = 0; t < ingestionThreads; t++) {
          ingestionThreadPool.submit(new IngestionRunnable(crawlController));
        }
        ingestionThreadPool.shutdown();
        try {
          ingestionThreadPool.awaitTermination(24, TimeUnit.HOURS);
        }
        catch (InterruptedException e) {
          logger.warn("Ingestion thread pool interrupted", e);
        }
        stopPipeline(connector);
      });
    }
    else {
      throw new IngestionException(String.format("Crawl on connector %s can't start because a previous crawl is running!", connector));
    }
  }

  private void startPipeline(String connector) {
    Connector conn = connectorManager.getConnector(connector);
    Pipeline pipeline = pipelineManager.getPipeline(conn.getRoute());
    pipeline.onStart(connector);
  }

  private void stopPipeline(String connector) {
    Connector conn = connectorManager.getConnector(connector);
    Pipeline pipeline = pipelineManager.getPipeline(conn.getRoute());
    pipeline.onStop(connector);
  }

  public void stopCrawl(String connector) {
    connectorManager.stopCrawl(connector, false);
  }

  private class IngestionRunnable implements Runnable {
    private CrawlController crawlController;

    IngestionRunnable(CrawlController crawlController) {
      this.crawlController = crawlController;
    }

    @Override
    public void run() {
      while (crawlController.isActive() || !crawlController.getFetchQueue().isEmpty()) {
        try {
          processQueue();
        }
        catch (InterruptedException e) {
          logger.warn("Interrupted while polling fetch queue", e);
        }
      }
    }

    private void processQueue() throws InterruptedException {
      CrawlItem item = crawlController.getFetchQueue().poll(1, TimeUnit.SECONDS);
      if (item != null) {
        Optional<Document> document = crawlController.getConnector().fetch(item);
        document.ifPresent(doc -> submitDocument(doc, crawlController.getConnector().getRoute()));
      }
    }
  }

  private void submitDocument(Document document, String route) {
    Pipeline pipeline = pipelineManager.getPipeline(route);
    if (pipeline != null) {
      pipeline.process(document);
    }
  }
}
