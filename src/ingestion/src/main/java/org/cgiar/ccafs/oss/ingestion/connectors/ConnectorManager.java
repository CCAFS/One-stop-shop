package org.cgiar.ccafs.oss.ingestion.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.CrawlController;
import org.cgiar.ccafs.oss.ingestion.core.IngestionException;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectorManager {
  private static final Logger logger = LogManager.getLogger(ConnectorManager.class);
  private Map<String, Connector> connectorMap = new ConcurrentHashMap<>();
  private Map<String, CrawlController> connectorControllers = new ConcurrentHashMap<>();
  public ConnectorManager(ObjectNode configuration) {
    logger.trace("Creating ConnectorManager");
    loadConfiguration(configuration);
  }

  private void loadConfiguration(ObjectNode configuration) {
    logger.trace("Loading configuration from %s", configuration.toString());
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
        connectorMap.put(connectorName.get().asText(), connector = instantiateConnector(connectorClass.get().asText()));
        connector.initialize(Optional.of((ObjectNode) elem.get("configuration")));
      } catch (Exception e) {
        logger.fatal("Error loading connector", e);
        throw new IngestionException("Error loading connector", e);
      }
    }
    else {
      String message = "Unable to load connector from JSON: " + elem.toString();
      logger.fatal(message);
      throw new IngestionException(message);
    }
  }

  private Connector instantiateConnector(String clazz) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    return (Connector) ConnectorManager.class.getClassLoader().loadClass(clazz).newInstance();
  }

  private Connector getConnector(String connectorName) {
    Connector connector = connectorMap.get(connectorName);
    if (connector == null) {
      throw new IngestionException(String.format("Connector %s is not configured", connectorName));
    }
    return connector;
  }

  public CrawlController startCrawl(String connectorName) {
    Connector connector = getConnector(connectorName);
    CrawlController crawlController = connectorControllers.putIfAbsent(connectorName, createCrawlController(connector));
    if (crawlController.isQuiescent()) {
      crawlController.startCrawl();
      return crawlController;
    }
    else {
      throw new IngestionException(String.format("Connector %s is already running a crawl", connectorName));
    }
  }

  private CrawlController getController(Connector connector) {
    Optional<CrawlController> crawlController = Optional.of(connectorControllers.get(connector.getName()));
    if (!crawlController.isPresent()) {
      throw new IngestionException(String.format("Error locating controller for connector %s", connector.getName()));
    }
    return crawlController.get();
  }

  private CrawlController createCrawlController(Connector connector) {
    CrawlController controller = new CrawlController();
    controller.initialize(connector, 3);
    return controller;
  }

  void stopCrawl(String connectorName) {
    Connector connector = getConnector(connectorName);
    CrawlController controller = getController(connector);
    controller.stopCrawl();
  }
}
