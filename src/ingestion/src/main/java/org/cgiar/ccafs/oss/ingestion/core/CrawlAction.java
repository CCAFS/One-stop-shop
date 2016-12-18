package org.cgiar.ccafs.oss.ingestion.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.connectors.Connector;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RecursiveAction;

/**
 * ===========================================================
 * Created by Eduardo Quiros-Campos - ArkiTechTura Consulting
 * ===========================================================
 **/
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
    logger.debug(String.format("Compute started on item %s", item.toString()));
    if (item.getType() == CrawlItem.ITEM_TYPE.ROOT) {
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
      try {
        context.newItem();
        fetchQueue.put(item);
      }
      catch (InterruptedException e) {
        throw new IngestionException(e);
      }
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
