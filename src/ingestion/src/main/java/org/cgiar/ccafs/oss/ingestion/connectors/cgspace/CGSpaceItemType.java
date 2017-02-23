package org.cgiar.ccafs.oss.ingestion.connectors.cgspace;

public enum CGSpaceItemType {
  COMMUNITY("Community"),
  COLLECTION("Collection"),
  ITEM("Item"),
  BITSTREAM("Bitstream");

  private final String value;

  CGSpaceItemType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
