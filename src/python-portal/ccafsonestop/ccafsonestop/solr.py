import pysolr

#Solr connection
solrConnection = pysolr.Solr('http://ec2-52-211-37-10.eu-west-1.compute.amazonaws.com:8080/solr/ccafs-oss', timeout=10)
