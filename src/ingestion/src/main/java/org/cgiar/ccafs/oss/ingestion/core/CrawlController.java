package org.cgiar.ccafs.oss.ingestion.core;

import org.cgiar.ccafs.oss.ingestion.connectors.Connector;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by equiros on 11/26/2016.
 */
public class CrawlController {
  private BlockingQueue<CrawlItem> fetchQueue;
  private Connector connector;
  private ForkJoinPool executorService;
  private CrawlStatus status;
  int numThreads;

  public void initialize(Connector connector, int numThreads) {
    this.connector = connector;
    this.numThreads = numThreads;
    this.fetchQueue = new LinkedBlockingQueue<>();
    executorService = new ForkJoinPool(numThreads);
    status = CrawlStatus.STOPPED;
  }

  public boolean isQuiescent() {
    return executorService.isQuiescent();
  }

  public void setStatus(CrawlStatus status) {
    this.status = status;
  }

  public ExecutorService startCrawl() {
    if (status == CrawlStatus.STOPPING) {
      throw new IngestionException(String.format("Crawl for connector %s is stopping", connector.getName()));
    }
    if (executorService.isShutdown()) {
      executorService = new ForkJoinPool(numThreads);
    }
    if (isQuiescent()) {
      if (!fetchQueue.isEmpty()) {
        throw new IngestionException(String.format("There are still pending items to claim from connector %s from a previous crawl", connector.getName()));
      }
      CrawlItem root = connector.getRootItem();
      status = CrawlStatus.RUNNING;
      executorService.invoke(new CrawlAction(connector, root, fetchQueue));
      return executorService;
    }
    else {
      throw new IngestionException(String.format("There is a crawl already running for %s", connector.getName()));
    }
  }

  public void stopCrawl(){
    if (!fetchQueue.isEmpty()) {
      fetchQueue.clear();
    }
    if (!executorService.isShutdown()) {
      executorService.shutdown();
      status = CrawlStatus.STOPPING;
    }
    else {
      status = CrawlStatus.STOPPED;
    }
  }

  public BlockingQueue<CrawlItem> getFetchQueue() {
    return fetchQueue;
  }

  public ForkJoinPool getExecutorService() {
    return executorService;
  }
}
