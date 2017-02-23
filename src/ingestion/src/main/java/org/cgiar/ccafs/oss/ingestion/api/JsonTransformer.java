package org.cgiar.ccafs.oss.ingestion.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {
  private ObjectMapper mapper = new ObjectMapper();
  public String render(Object o) throws Exception {
    return mapper.writeValueAsString(o);
  }
}
