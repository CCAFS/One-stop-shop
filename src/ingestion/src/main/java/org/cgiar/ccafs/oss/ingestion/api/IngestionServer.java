package org.cgiar.ccafs.oss.ingestion.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.IngestionController;
import org.cgiar.ccafs.oss.ingestion.core.IngestionException;

import static spark.Spark.*;

/**
 * ===========================================================
 * Created by Eduardo Quiros-Campos - ArkiTechTura Consulting
 * ===========================================================
 **/
public class IngestionServer {
  private static final Logger logger = LogManager.getLogger(IngestionServer.class);
  private static JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(false);

  public static void main(String[] args) {
    IngestionController controller = null;
    if (args.length == 1) {
      try {
        logger.info("Attempting to bootstrap Ingestion Server ...");
        controller = IngestionController.fromDirectory(args[0]);
      }
      catch (IngestionException e) {
        logger.fatal(String.format("Error starting Ingestion Server: %s", e.getMessage()), e);
      }
    }
    else {
      System.out.println("Usage: IngestionServer <configuration directory>");
      System.exit(1);
    }
    if (controller == null) {
      logger.fatal("Error starting Ingestion Server");
      System.exit(1);
    }
    final IngestionController ingestionController = controller;
    get("/connector/:connector/start",
            (request, response) -> {
              try {
                ingestionController.startCrawl(request.params(":connector"));
                return requestOK();
              }
              catch (IngestionException e) {
                return requestError(e);
              }
            });
    get("/connector/:connector/stop",
            (request, response) -> {
              try {
                ingestionController.stopCrawl(request.params(":connector"));
                return requestOK();
              }
              catch (IngestionException e) {
                return requestError(e);
              }
            });
  }

  private static String requestError(IngestionException e) {
    ObjectNode ret = jsonNodeFactory.objectNode();
    ret.put("Acknowledge", "false");
    ret.put("Message", e.getMessage());
    return ret.toString();
  }

  private static String requestOK() {
    ObjectNode ret = jsonNodeFactory.objectNode();
    ret.put("Acknowledge", "true");
    return ret.toString();
  }
}
