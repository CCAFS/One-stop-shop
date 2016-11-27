package org.cgiar.ccafs.oss.ingestion.core;

import com.google.common.base.Optional;

import java.net.URI;

/**
 * Created by equiros on 11/26/2016.
 */
public class CrawlItem {
  public enum ITEM_TYPE {ROOT, CONTAINER, LEAF}
  private ITEM_TYPE type;
  private URI uri;
  private String name;
  private Optional<CrawlItem> parent = Optional.absent();

  public CrawlItem(String name, ITEM_TYPE type, URI uri, CrawlItem parent) {
    this.name = name;
    this.type = type;
    this.uri = uri;
    this.parent = Optional.of(parent);
  }

  public CrawlItem(String name, ITEM_TYPE type, URI uri) {
    this(name, type, uri, null);
  }

  public ITEM_TYPE getType() {
    return type;
  }

  public URI getUri() {
    return uri;
  }

  public String getName() {
    return name;
  }

  public Optional<CrawlItem> getParent() {
    return parent;
  }
}
