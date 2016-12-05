package org.cgiar.ccafs.oss.ingestion.api;

import org.cgiar.ccafs.oss.ingestion.core.IngestionController;

import static spark.Spark.*;

/**
 * ===========================================================
 * Created by Eduardo Quiros-Campos - ArkiTechTura Consulting
 * ===========================================================
 **/
public class ApiEntryPoint {
  public static void main(String[] args) {
    IngestionController controller = null;
    if (args.length == 1) {
      controller = new IngestionController(args[0]);
    }
    else {
      System.out.println("Usage: ApiEntryPoint <configuration directory>");
      System.exit(1);
    }
    final IngestionController finalController = controller;
    get("/connector/:connector/:operation", (req, res) -> finalController.toString());
  }
}
