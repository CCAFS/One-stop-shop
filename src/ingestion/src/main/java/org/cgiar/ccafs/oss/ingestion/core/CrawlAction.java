package org.cgiar.ccafs.oss.ingestion.core;

import org.cgiar.ccafs.oss.ingestion.connectors.Connector;

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

  public CrawlAction(Connector connector, CrawlItem item, BlockingQueue<CrawlItem> fetchQueue) {
    this.connector = connector;
    this.item = item;
    this.fetchQueue = fetchQueue;
  }

  protected void compute() {
    if (item.getType() == CrawlItem.ITEM_TYPE.ROOT) {
      List<CrawlItem> discoveredItems = new LinkedList<CrawlItem>();
      connector.crawlRoot(item, discoveredItems);
      invokeAll(toActionList(discoveredItems));
    }
    else if (item.getType() == CrawlItem.ITEM_TYPE.CONTAINER) {
      List<CrawlItem> discoveredItems = new LinkedList<CrawlItem>();
      connector.scan(item, discoveredItems);
      invokeAll(toActionList(discoveredItems));
    }
    else {
      try {
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
      actions.add(new CrawlAction(connector, it, fetchQueue));
    }
    return actions;
  }
}
