package org.cgiar.ccafs.oss.ingestion.stages.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.cgiar.ccafs.oss.ingestion.core.Document;
import org.cgiar.ccafs.oss.ingestion.core.IngestionException;
import org.cgiar.ccafs.oss.ingestion.core.stages.Stage;
import org.cgiar.ccafs.oss.ingestion.util.ConfigurationUtilities;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class BetterSolrPublisher implements Stage {
  private HttpSolrClient solrClient;
  private Logger logger = LogManager.getLogger(BetterSolrPublisher.class);
  private AtomicInteger commitCounter = new AtomicInteger(0);
  private int batchSize;

  public BetterSolrPublisher() {

  }
  @Override
  public void initialize(ObjectNode configuration) {
    String solrUrl = ConfigurationUtilities.safeString(configuration, "solrUrl", "");
    String solrCore = ConfigurationUtilities.safeString(configuration, "solrCore", "");
    batchSize = ConfigurationUtilities.safeInteger(configuration, "batchSize", 100);
    if (solrUrl.isEmpty() || solrCore.isEmpty()) {
      throw new IngestionException("Either URL or Core are empty on SolrPublisher stage");
    }
    solrClient = new HttpSolrClient(solrUrl + "/" + solrCore);
  }

  @Override
  public Optional<Document> process(Document document) {
    SolrInputDocument solrDocument = new SolrInputDocument();
    addFields(solrDocument, document);
    try {
      solrClient.add(solrDocument);
      tryCommit();
    }
    catch (SolrServerException | IOException e) {
      String msg = "Error adding SolrDcument to SolrClient";
      logger.fatal(msg, e);
      throw new IngestionException(msg, e);
    }
    return Optional.of(document);
  }

  private void tryCommit() {
    if (commitCounter.incrementAndGet() > batchSize) {
      try {
        solrClient.commit();
      }
      catch (SolrServerException | IOException e) {
        String msg = "Error committing to Solr";
        logger.fatal(msg, e);
        throw new IngestionException(msg, e);
      }
      finally {
        commitCounter.set(0);
      }
    }
  }

  private void addFields(SolrInputDocument solrDocument, Document document) {
    ArrayNode solrData = (ArrayNode) document.getScope("Solr").get();
    for (Iterator<JsonNode> it = solrData.elements(); it.hasNext(); ) {
      ObjectNode node = (ObjectNode) it.next();
      solrDocument.addField(node.get("fieldName").asText(), node.get("fieldValue").asText());
    }
  }

  @Override
  public void onStart(String connector) {
    commitCounter.set(0);
  }

  @Override
  public void onFinish(String connector) {
    try {
      solrClient.commit();
    }
    catch (SolrServerException | IOException e) {
      String msg = "Error committing SolrClient";
      logger.fatal(msg, e);
      throw new IngestionException(msg, e);
    }
  }
}
