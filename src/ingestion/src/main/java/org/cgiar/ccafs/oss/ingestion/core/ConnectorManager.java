package org.cgiar.ccafs.oss.ingestion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.connectors.Connector;
import org.cgiar.ccafs.oss.ingestion.util.ConfigurationUtilities;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectorManager {
  private static final Logger logger = LogManager.getLogger(ConnectorManager.class);
  private Map<String, Connector> connectors = new ConcurrentHashMap<>();
  private Map<String, CrawlController> connectorControllers = new ConcurrentHashMap<>();
  private static JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);
  private ObjectNode configuration;

  public ConnectorManager(ObjectNode configuration) {
    logger.debug("Creating ConnectorManager");
    this.configuration = configuration;
    initialize();
  }

  private void initialize() {
    logger.debug(String.format("Loading configuration from %s", configuration.toString()));
    Optional<ArrayNode> connectors = Optional.of((ArrayNode) configuration.get("connectors"));
    if (connectors.isPresent()) {
      for (Iterator<JsonNode> it = connectors.get().elements(); it.hasNext(); ) {
        ObjectNode elem = (ObjectNode) it.next();
        loadConnector(elem);
      }
    }
    else {
      logger.warn("There are no connectors configured");
    }
  }

  private void loadConnector(ObjectNode elem) {
    Optional<JsonNode> connectorName = Optional.of(elem.get("name"));
    Optional<JsonNode> connectorClass = Optional.of(elem.get("class"));
    if (connectorName.isPresent() && connectorClass.isPresent()) {
      try {
        Connector connector;
        connectors.put(connectorName.get().asText(), connector = instantiateConnector(connectorClass.get().asText()));
        connector.setName(connectorName.get().asText());
        connector.initialize((ObjectNode) elem.get("configuration"));
      } catch (Exception e) {
        String msg = "Error loading connector";
        logger.fatal(msg, e);
        throw new IngestionException(msg, e);
      }
    }
    else {
      String message = "Unable to load connector from configuration: " + elem.toString();
      logger.fatal(message);
      throw new IngestionException(message);
    }
  }

  private Connector instantiateConnector(String clazz) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    return (Connector) ConnectorManager.class.getClassLoader().loadClass(clazz).newInstance();
  }

  public Connector getConnector(String connectorName) {
    Connector connector = connectors.get(connectorName);
    if (connector == null) {
      throw new IngestionException(String.format("Connector %s is not configured", connectorName));
    }
    return connector;
  }

  synchronized ObjectNode crawlStatus(String connectorName) {
    ObjectNode status = jsonNodeFactory.objectNode();
    Connector connector = getConnector(connectorName);
    status.put("connector", connectorName);
    CrawlController crawlController = getController(connector);
    if (crawlController == null) {
      status.put("message", String.format("Connector %s is currently not running a crawl", connectorName));
    }
    CrawlContext crawlContext = crawlController.getCrawlContext();
    status.put("crawlStatus", crawlController.getStatus().toString());
    status.put("containerCount", crawlContext.getContainerCount());
    status.put("itemCount", crawlContext.getItemCount());
    status.put("errorCount", crawlContext.getErrorCount());
    status.put("fetchQueueLength", crawlController.getFetchQueue().size());
    return status;
  }

  synchronized CrawlController startCrawl(String connectorName, int limit) {
    if (canStartCrawl(connectorName)) {
      Connector connector = getConnector(connectorName);
      CrawlController crawlController = getController(connector);
      if (crawlController == null) {
        connectorControllers.put(connectorName, crawlController = createCrawlController(connector));
      }
      crawlController.startCrawl(limit);
      return crawlController;
    }
    else {
      throw new IngestionException(String.format("Connector %s is already running a crawl", connectorName));
    }
  }

  public CrawlController getController(String connectorName) {
    Connector connector = getConnector(connectorName);
    return getController(connector);
  }

  synchronized boolean canStartCrawl(String connectorName) {
    Connector connector = getConnector(connectorName);
    CrawlController controller = getController(connector);
    return controller == null || (controller.isQuiescent() && controller.getFetchQueue().isEmpty());
  }

  private CrawlController getController(Connector connector) {
    return connectorControllers.get(connector.getName());
  }

  private CrawlController createCrawlController(Connector connector) {
    CrawlController controller = new CrawlController();
    controller.initialize(connector, ConfigurationUtilities.safeInteger(this.configuration, "crawlThreads", 5));
    return controller;
  }

  void stopCrawl(String connectorName, boolean force) {
    Connector connector = getConnector(connectorName);
    CrawlController controller = getController(connector);
    controller.stopCrawl(force);
  }
}
