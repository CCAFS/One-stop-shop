package org.cgiar.ccafs.oss.ingestion.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.connectors.Connector;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RecursiveAction;

public class CrawlAction extends RecursiveAction {
  private Connector connector;
  private CrawlItem item;
  private BlockingQueue<CrawlItem> fetchQueue;
  private static final Logger logger = LogManager.getLogger(CrawlAction.class);
  private CrawlContext context;

  public CrawlAction(CrawlContext context, Connector connector, CrawlItem item, BlockingQueue<CrawlItem> fetchQueue) {
    this.context = context;
    this.connector = connector;
    this.item = item;
    this.fetchQueue = fetchQueue;
    logger.debug(String.format("CrawlAction created with item %s", item.toString()));
  }

  protected void compute() {
    if (!context.shouldProceed()) return;
    if (item.getType() == CrawlItem.ITEM_TYPE.ROOT) {
      logger.info(String.format("Processing root item: %s", item.toString()));
      List<CrawlItem> discoveredItems = new LinkedList<CrawlItem>();
      try {
        connector.crawlRoot(item, discoveredItems);
        invokeAll(toActionList(discoveredItems));
      }
      catch (IngestionException e) {
        logger.debug("Error crawling root", e);
      }
    }
    else if (item.getType() == CrawlItem.ITEM_TYPE.CONTAINER) {
      logger.info(String.format("Processing container: %s", item.toString()));
      List<CrawlItem> discoveredItems = new LinkedList<CrawlItem>();
      try {
        context.newContainer();
        connector.scan(item, discoveredItems);
        invokeAll(toActionList(discoveredItems));
      }
      catch (IngestionException e) {
        logger.debug(String.format("Error crawling item %s", item.getUri().toString()), e);
      }
    }
    else {
      logger.info(String.format("Adding item %s to fetch queue", item.toString()));
      context.newItem();
      fetchQueue.add(item);
    }
  }

  private List<CrawlAction> toActionList(List<CrawlItem> discoveredItems) {
    List<CrawlAction> actions = new ArrayList<CrawlAction>(discoveredItems.size());
    for (CrawlItem it : discoveredItems) {
      actions.add(new CrawlAction(context, connector, it, fetchQueue));
    }
    return actions;
  }

  public CrawlContext getContext() {
    return context;
  }
}
