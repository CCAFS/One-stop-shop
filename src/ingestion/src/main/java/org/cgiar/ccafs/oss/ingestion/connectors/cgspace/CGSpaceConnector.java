package org.cgiar.ccafs.oss.ingestion.connectors.cgspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cgiar.ccafs.oss.ingestion.core.CrawlController;
import org.cgiar.ccafs.oss.ingestion.core.CrawlItem;
import org.cgiar.ccafs.oss.ingestion.core.Document;
import org.cgiar.ccafs.oss.ingestion.core.IngestionException;
import org.cgiar.ccafs.oss.ingestion.core.connectors.Connector;
import org.cgiar.ccafs.oss.ingestion.util.ConfigurationUtilities;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class CGSpaceConnector implements Connector {
  private CloseableHttpClient httpClient;
  private String connectorName;
  private String baseURL;
  private String root;
  private String route;
  private static Logger logger = LogManager.getLogger(CGSpaceConnector.class);

  private final String ITEM_TYPE_KEY = "CGSPACE-ITEM-TYPE";

  public CGSpaceConnector() {
    httpClient = HttpClients.createDefault();
  }

  @Override
  public void setName(String name) {
    this.connectorName = name;
  }

  @Override
  public String getName() {
    return connectorName;
  }

  @Override
  public void initialize(Optional<ObjectNode> configuration) {
    if (!configuration.isPresent()) {
      throw new IngestionException("Can not initialize CGSpace Connector because there is no configuration present");
    }
    this.baseURL = ConfigurationUtilities.safeString(configuration.get(), "baseURL", null);
    if (this.baseURL == null) {
      throw new IngestionException("Can not initialize CGSpace Connector because there is no Base URL");
    }
    this.root = ConfigurationUtilities.safeString(configuration.get(), "root", "/communities");
    this.route = ConfigurationUtilities.safeString(configuration.get(), "route", "default");
  }

  @Override
  public CrawlItem getRootItem() {
    try {
      CrawlItem rootItem = new CrawlItem("root", CrawlItem.ITEM_TYPE.ROOT, ConfigurationUtilities.buildURI(baseURL + this.root));
      rootItem.addMetadata(ITEM_TYPE_KEY, detectRootType(rootItem.getUri()));
      return rootItem;
    }
    catch (IOException e) {
      logger.fatal("Error detecting root item type", e);
      throw new IngestionException("Error detecting root item type", e);
    }
  }

  private CGSpaceItemType detectRootType(URI uri) throws IOException {
    JsonNode content = readContent(uri);
    ObjectNode head = (ObjectNode) (content.isArray() ? content.get(0) : content);
    String type = head.get("type").asText();
    switch (type) {
      case "community": return CGSpaceItemType.COMMUNITY;
      case "collection": return CGSpaceItemType.COLLECTION;
      case "item": return CGSpaceItemType.ITEM;
      default: return CGSpaceItemType.BITSTREAM;
    }
  }

  private JsonNode readContent(URI uri, Map<String, String> headers) throws IOException {
    HttpGet request = new HttpGet(uri);
    request.setHeader(HttpHeaders.ACCEPT, "application/json");
    if (headers != null) {
      for (Map.Entry<String, String> h : headers.entrySet()) {
        request.setHeader(h.getKey(), h.getValue());
      }
    }
    HttpResponse response = httpClient.execute(request);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode content = null;
    content = mapper.readTree(response.getEntity().getContent());
    return content;
  }

  private JsonNode readContent(URI uri) throws IOException {
    return readContent(uri, null);
  }

  @Override
  public void crawlRoot(CrawlItem rootItem, List<CrawlItem> discoveredItems) {
    try {
      CGSpaceItemType type = rootItem.getMetadata(ITEM_TYPE_KEY);
      URI finalUri = new URIBuilder(rootItem.getUri().toString()).addParameter("limit", "1000").build();
      JsonNode content = readContent(finalUri);
      switch (type) {
        case COMMUNITY:
          if (content.isArray()) {
            ArrayNode communities = (ArrayNode) content;
            for (Iterator<JsonNode> it = communities.elements(); it.hasNext();) {
              JsonNode communityNode = it.next();
              CrawlItem communityItem = createCommunityItem(rootItem, communityNode);
              if (communityItem != null) {
                discoveredItems.add(communityItem);
              }
            }
          }
          else {
            CrawlItem communityItem = createCommunityItem(rootItem, content);
            if (communityItem != null) {
              discoveredItems.add(communityItem);
            }
          }
          break;
        default:
          //TODO: Support additional item types as crawl roots
          throw new IngestionException(String.format("Element of type %s is not supported as root item", type.toString()));
      }
    }
    catch (URISyntaxException | IOException e) {
      e.printStackTrace();
    }
  }

  private CrawlItem createCommunityItem(CrawlItem rootItem, JsonNode communityNode) {
    if (!communityNode.isObject()) {
      logger.warn(String.format("Unexpected content while processing community: %s", communityNode.toString()));
    }
    ObjectNode community = (ObjectNode) communityNode;
    if (community.has("id") && community.has("link")) {
      CrawlItem communityItem = new CrawlItem(ConfigurationUtilities.safeString(community, "name", "<<Community Name Not Present>>"),
              CrawlItem.ITEM_TYPE.CONTAINER, ConfigurationUtilities.buildURI(baseURL + community.get("link").asText()), rootItem);
      communityItem.addMetadata(ITEM_TYPE_KEY, CGSpaceItemType.COMMUNITY);
      return communityItem;
    }
    else {
      logger.warn(String.format("Community node %s does not have the required id & link fields", community.toString()));
      return null;
    }
  }

  @Override
  public void scan(CrawlItem containerItem, List<CrawlItem> discoveredItems) {
    CGSpaceItemType type = containerItem.getMetadata(ITEM_TYPE_KEY);
    switch (type) {
      case COMMUNITY:
        try {
          crawlCommunity(containerItem, discoveredItems);
        }
        catch (URISyntaxException | IOException e) {
          logger.warn(String.format("Error crawling community item %s", containerItem.toString()), e);
        }
        break;
      case COLLECTION:
        try {
          crawlCollection(containerItem, discoveredItems);
        }
        catch (IOException | URISyntaxException e) {
          logger.warn(String.format("Error crawling collection item %s", containerItem.toString()), e);
        }
        break;
      default:
        logger.warn(String.format("Only items of type Community and Collection can be crawled as containers. Found: %s", type.toString()));
    }
  }

  private void crawlCollection(CrawlItem collectionItem, List<CrawlItem> discoveredItems) throws IOException, URISyntaxException {
    // TODO: batch requests to avoid setting limit to 10000
    URI uri = new URIBuilder(collectionItem.getUri())
            .addParameter("expand", "items")
            .addParameter("limit", "10000").build();
    JsonNode content = readContent(uri);
    processCollection(collectionItem, content, discoveredItems);
  }

  private void processCollection(CrawlItem collectionItem, JsonNode content, List<CrawlItem> discoveredItems) {
    JsonNode itemsNode = content.get("items");
    if (itemsNode != null) {
      ArrayNode items = (ArrayNode) itemsNode;
      for (Iterator<JsonNode> nodes = items.elements(); nodes.hasNext(); ) {
        CrawlItem item = createItem(collectionItem, nodes.next());
        if (item != null) {
          discoveredItems.add(item);
        }
      }
    }
  }

  private CrawlItem createItem(CrawlItem parent, JsonNode itemNode) {
    if (!itemNode.isObject()) {
      throw new IngestionException("Unexpected content while processing item");
    }
    ObjectNode item = (ObjectNode) itemNode;
    if (item.has("id") && item.has("link")) {
      CrawlItem crawlItem = new CrawlItem(ConfigurationUtilities.safeString(item, "name", "<<Item Name Not Present>>"),
              CrawlItem.ITEM_TYPE.LEAF, ConfigurationUtilities.buildURI(baseURL + item.get("link").asText()), parent);
      crawlItem.addMetadata(ITEM_TYPE_KEY, CGSpaceItemType.ITEM);
      return crawlItem;
    }
    else {
      logger.warn(String.format("Item node %s does not have the required id & link fields", item.toString()));
      return null;
    }
  }

  private void crawlCommunity(CrawlItem communityItem, List<CrawlItem> discoveredItems) throws URISyntaxException, IOException {
    // TODO: batch requests to avoid setting limit to 1000
    URI uri = new URIBuilder(communityItem.getUri())
            .addParameter("expand", "collections")
            .addParameter("limit", "1000").build();
    JsonNode content = readContent(uri);
    processCommunity(communityItem, content, discoveredItems);
  }

  private void processCommunity(CrawlItem parent, JsonNode content, List<CrawlItem> discoveredItems) {
    JsonNode collectionsNode = content.get("collections");
    if (collectionsNode != null) {
      ArrayNode collections = (ArrayNode) collectionsNode;
      for (Iterator<JsonNode> nodes = collections.elements(); nodes.hasNext(); ) {
        CrawlItem item = createCollectionItem(parent, nodes.next());
        if (item != null) {
          discoveredItems.add(item);
        }
      }
    }
  }

  private CrawlItem createCollectionItem(CrawlItem parent, JsonNode collectionNode) {
    if (!collectionNode.isObject()) {
      logger.warn(String.format("Unexpected content while processing collection node: %s", collectionNode.toString()));
      return null;
    }
    ObjectNode collection = (ObjectNode) collectionNode;
    if (collection.has("id") && collection.has("link")) {
      CrawlItem crawlItem = new CrawlItem(ConfigurationUtilities.safeString(collection, "name", "<<Collection Name Not Present>>"),
              CrawlItem.ITEM_TYPE.CONTAINER, ConfigurationUtilities.buildURI(baseURL + collection.get("link").asText()), parent);
      crawlItem.addMetadata(ITEM_TYPE_KEY, CGSpaceItemType.COLLECTION);
      return crawlItem;
    }
    else {
      logger.warn(String.format("Collection node %s does not have the required id & link fields", collection.toString()));
      return null;
    }
  }

  @Override
  public Optional<Document> fetch(CrawlItem leafItem) {
    try {
      URI uri = new URIBuilder(leafItem.getUri())
              .addParameter("expand", "metadata").build();
      JsonNode content = readContent(uri);
      return processItem(leafItem, content);
    }
    catch (URISyntaxException | IOException e) {
      logger.warn(String.format("Error encountered while fetching item %s", leafItem.toString()), e);
      return Optional.empty();
    }
  }

  @Override
  public String getRoute() {
    return null;
  }

  private Optional<Document> processItem(CrawlItem leafItem, JsonNode content) {
    JsonNode metadata = content.get("metadata");
    if (!metadata.isObject()) {
      logger.warn(String.format("Metadata not found on item %s", leafItem.toString()));
      return Optional.empty();
    }
    Document doc = new Document();
    doc.setScope("CSSpaceMetadata", (ObjectNode) metadata);
    return Optional.of(doc);
  }
}
