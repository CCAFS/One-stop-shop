package org.cgiar.ccafs.oss.ingestion.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.IngestionController;
import org.cgiar.ccafs.oss.ingestion.core.IngestionException;

import java.io.File;

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
    Options options = setupOptions();
    logger.info("Attempting to bootstrap Ingestion Server ...");
    CommandLineParser parser = new DefaultParser();
    IngestionController controller = null;
    try {
      CommandLine commandLine = parser.parse( options, args );
      if (commandLine.hasOption("directory")) {
        controller = IngestionController.fromDirectory(commandLine.getOptionValue("directory"));
      }
      else if (commandLine.hasOption("file")) {
        controller = IngestionController.fromFile(new File(commandLine.getOptionValue("file")));
      }
    }
    catch( ParseException e ) {
      logger.fatal( "Argument parsing failed", e);
    }
    if (controller == null) {
      printHelp(options);
      System.exit(1);
    }
    final IngestionController ingestionController = controller;
    get("/connector/:connector/start",
            (request, response) -> {
              try {
                ingestionController.startCrawl(request.params(":connector"), -1);
                response.type("application/json");
                return requestOK();
              }
              catch (IngestionException e) {
                response.status(400);
                response.type("application/json");
                return requestError(e);
              }
            });
    get("/connector/:connector/test",
            (request, response) -> {
              try {
                ingestionController.startCrawl(request.params(":connector"), 20);
                response.type("application/json");
                return requestOK();
              }
              catch (IngestionException e) {
                response.status(400);
                response.type("application/json");
                return requestError(e);
              }
            });
    get("/connector/:connector/stop",
            (request, response) -> {
              try {
                ingestionController.stopCrawl(request.params(":connector"));
                response.type("application/json");
                return requestOK();
              }
              catch (IngestionException e) {
                response.status(400);
                response.type("application/json");
                return requestError(e);
              }
            });
    get("/connector/:connector/status",
            (request, response) -> {
              try {
                ObjectNode status = ingestionController.ingestionStatus(request.params(":connector"));
                response.type("application/json");
                return requestOK(status);
              }
              catch (IngestionException e) {
                response.status(400);
                response.type("application/json");
                return requestError(e);
              }
            });
    get("/shutdown/now",
            (request, response) -> {
              logger.info("Shutting down immediately!");
              response.type("application/json");
              System.exit(-1);
              return "This should not be returned";
            });
  }

  private static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp( "IngestionServer", options );
  }

  private static Options setupOptions() {
    Option directory = Option.builder("d").longOpt("directory").hasArg().argName("directory").desc("Directory name to locate 'config.json'").build();
    Option file = Option.builder("f").longOpt("file").hasArg().argName("file").desc("File to use for server configuration").build();
    Options options = new Options();
    options.addOption(directory);
    options.addOption(file);
    return options;
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

  private static String requestOK(ObjectNode status) {
    status.put("Acknowledge", "true");
    return status.toString();
  }
}
