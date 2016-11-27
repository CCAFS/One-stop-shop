package org.cgiar.ccafs.oss.ingestion.core;

import org.cgiar.ccafs.oss.ingestion.connectors.Connector;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * Created by equiros on 11/26/2016.
 */
public interface CrawlController {
  void initialize(Connector connector, BlockingQueue<CrawlItem> fetchQueue);
  ExecutorService startCrawl(int numThreads);
}
