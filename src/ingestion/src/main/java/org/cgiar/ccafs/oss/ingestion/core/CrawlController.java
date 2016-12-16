package org.cgiar.ccafs.oss.ingestion.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.connectors.Connector;

import java.util.concurrent.*;

/**
 * Created by equiros on 11/26/2016.
 */
public class CrawlController {
  private BlockingQueue<CrawlItem> fetchQueue;
  private Connector connector;
  private ForkJoinPool executorService;
  private volatile CrawlStatus status;
  int numThreads;
  private Thread watchdog;
  private static final Logger logger = LogManager.getLogger(CrawlController.class);

  public void initialize(Connector connector, int numThreads) {
    this.connector = connector;
    this.numThreads = numThreads;
    this.fetchQueue = new LinkedBlockingQueue<>();
    status = CrawlStatus.STOPPED;
    logger.debug("CrawlController initialized");
  }

  private synchronized void setStatus(CrawlStatus newStatus) {
    logger.debug(String.format("Setting CrawlController status to %s", newStatus.toString()));
    this.status = newStatus;
  }

  public boolean isQuiescent() {
    return status == CrawlStatus.STOPPED || status == CrawlStatus.DONE || status == CrawlStatus.ERROR;
  }

  public ExecutorService startCrawl() {
    if (status == CrawlStatus.STOPPING) {
      logger.debug("Attempted START on CrawlController while in STOPPING state");
      throw new IngestionException(String.format("Crawl for connector %s is stopping", connector.getName()));
    }
    else if (status == CrawlStatus.STOPPED || status == CrawlStatus.DONE) {
      logger.debug("CrawlController STARTING");
      executorService = new ForkJoinPool(numThreads);
    }
    else if (status == CrawlStatus.RUNNING) {
      logger.debug(" Attempted START on CrawlController while RUNNING");
      throw new IngestionException(String.format("There is a crawl already running for %s", connector.getName()));
    }
    if (!fetchQueue.isEmpty()) {
      logger.debug("Attempted START on CrawlController while fetch queue is not empty");
      throw new IngestionException(String.format("There are still pending items to claim from connector %s from a previous crawl", connector.getName()));
    }
    CrawlItem root = connector.getRootItem();
    setStatus(CrawlStatus.RUNNING);
    CrawlAction rootAction = new CrawlAction(connector, root, fetchQueue);
    executorService.submit(rootAction);
    startWatchdogThread(rootAction, this);
    return executorService;
  }

  void join() {
    if (status == CrawlStatus.RUNNING || status == CrawlStatus.STOPPING) {
      try {
        watchdog.join();
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void startWatchdogThread(final CrawlAction rootAction, final CrawlController cc) {
    watchdog = new Thread(() -> {
      try {
        rootAction.join();
        cc.setStatus(CrawlStatus.DONE);
      }
      catch (Exception e) {
        cc.setStatus(CrawlStatus.ERROR);
      }
    });
    watchdog.start();
  }

  public void stopCrawl(boolean force){
    if (!fetchQueue.isEmpty()) {
      fetchQueue.clear();
    }
    if (watchdog != null) {
      watchdog.interrupt();
    }
    if (!executorService.isShutdown()) {
      if (!force) {
        executorService.shutdown();
        setStatus(CrawlStatus.STOPPING);
      }
      else {
        executorService.shutdownNow();
        setStatus(CrawlStatus.STOPPED);
      }
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
