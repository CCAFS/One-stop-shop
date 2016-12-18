package org.cgiar.ccafs.oss.ingestion.core;

public class CrawlContext {
  private volatile long itemCount = 0;
  private volatile long containerCount = 0;
  private volatile long errorCount = 0;
  private long limit;

  CrawlContext(long limit) {
    this.limit = limit;
  }

  CrawlContext() {
    this(-1);
  }

  synchronized void newItem() {
    itemCount++;
  }

  synchronized void newContainer() {
    containerCount++;
  }

  synchronized void newError() {
    errorCount++;
  }

  boolean shouldProceed() {
    return limit == -1 || itemCount <= limit;
  }

  public long getItemCount() {
    return itemCount;
  }

  public long getContainerCount() {
    return containerCount;
  }

  public long getErrorCount() {
    return errorCount;
  }
}
