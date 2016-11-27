package org.cgiar.ccafs.oss.ingestion.core;

/**
 * ===========================================================
 * Created by Eduardo Quiros-Campos - ArkiTechTura Consulting
 * ===========================================================
 **/
public class IngestionException extends RuntimeException {
  public IngestionException() {
  }

  public IngestionException(String message) {
    super(message);
  }

  public IngestionException(String message, Throwable cause) {
    super(message, cause);
  }

  public IngestionException(Throwable cause) {
    super(cause);
  }

  public IngestionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
