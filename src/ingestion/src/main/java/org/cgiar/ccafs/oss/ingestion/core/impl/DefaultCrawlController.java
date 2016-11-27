package org.cgiar.ccafs.oss.ingestion.core.impl;

import org.cgiar.ccafs.oss.ingestion.connectors.Connector;
import org.cgiar.ccafs.oss.ingestion.connectors.CrawlAction;
import org.cgiar.ccafs.oss.ingestion.core.CrawlController;
import org.cgiar.ccafs.oss.ingestion.core.CrawlItem;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * ===========================================================
 * Created by Eduardo Quiros-Campos - ArkiTechTura Consulting
 * ===========================================================
 **/
public class DefaultCrawlController implements CrawlController {
  private BlockingQueue<CrawlItem> fetchQueue;
  private Connector connector;
  public void initialize(Connector connector, BlockingQueue<CrawlItem> fetchQueue) {
    this.connector = connector;
    this.fetchQueue = fetchQueue;
  }

  public ExecutorService startCrawl(int numThreads) {
    CrawlItem root = connector.getRootItem();
    ForkJoinPool executor = new ForkJoinPool(numThreads);
    executor.invoke(new CrawlAction(connector, root, fetchQueue));
    return executor;
  }

}
