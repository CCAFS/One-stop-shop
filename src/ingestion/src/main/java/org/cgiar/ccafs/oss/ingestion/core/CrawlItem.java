package org.cgiar.ccafs.oss.ingestion.core;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by equiros on 11/26/2016.
 */
public class CrawlItem {
  public enum ITEM_TYPE {ROOT, CONTAINER, LEAF}
  private ITEM_TYPE type;
  private URI uri;
  private String name;
  private Optional<CrawlItem> parent = Optional.empty();
  private Map<String, Object> metadata;

  public CrawlItem(String name, ITEM_TYPE type, URI uri, CrawlItem parent) {
    this.name = name;
    this.type = type;
    this.uri = uri;
    this.parent = Optional.of(parent);
    this.metadata = new HashMap<>();
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

  public CrawlItem child(String name, ITEM_TYPE type, URI uri) {
    return new CrawlItem(name, type, uri, this);
  }

  public void addMetadata(String key, Object value) {
    metadata.put(key, value);
  }

  public boolean hasMetadata(String key) {
    return metadata.containsKey(key);
  }

  public <T> T getMetadata(String key) {
    Object value = metadata.get(key);
    return (T) value;
  }

  @Override
  public String toString() {
    return String.format("<<URI:%s>>", this.uri.toString());
  }
}
