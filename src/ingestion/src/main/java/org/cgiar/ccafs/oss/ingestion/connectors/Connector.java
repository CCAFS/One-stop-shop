package org.cgiar.ccafs.oss.ingestion.connectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cgiar.ccafs.oss.ingestion.core.CrawlController;
import org.cgiar.ccafs.oss.ingestion.core.CrawlItem;
import org.cgiar.ccafs.oss.ingestion.core.Document;

import java.util.List;
import java.util.Optional;

/**
 * Created by equiros on 11/26/2016.
 */
public interface Connector {
  void setName(String name);
  String getName();
  void initialize(Optional<ObjectNode> configuration);
  CrawlItem getRootItem();
  void crawlRoot(CrawlItem rootItem, List<CrawlItem> discoveredItems);
  void scan(CrawlItem containerItem, List<CrawlItem> discoveredItems);
  Document fetch(CrawlItem leafItem, CrawlController crawlController);
}
