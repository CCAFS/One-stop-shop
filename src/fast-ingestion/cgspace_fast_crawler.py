import requests
import sys
import logging
import json
import uuid

logger = logging.getLogger('cgspace:fast:crawler')
logger.setLevel(logging.DEBUG)
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)
# create formatter and add it to the handlers
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)
# add the handlers to the logger
logger.addHandler(ch)



def get_communities(base_url):
    '''
    Returns the list of communities
    :param base_url: base URL for crawling
    :return: a list of triplets (community id, community name, item count) for each community discovered
    '''
    headers = {'Accept': 'application/json'}
    logger.debug("Getting communities list")
    r = requests.get(base_url + "/communities", headers=headers)
    json = r.json()
    logger.debug("Obtained {0:d} communities".format(len(json)))
    # return [(comm['id'], comm['name'], comm['countItems']) for comm in json]
    return [(62, 'CCAFS', 2918)]


def get_collections(base_url, community):
    headers = {'Accept': 'application/json'}
    params = {'expand': 'collections'}
    logger.debug("Getting collections for community {0:d}".format(community[0]))
    r = requests.get(base_url + "/communities/{0:d}".format(community[0]), headers=headers, params=params)
    json = r.json()
    logger.debug("Found {0:d} collections for community {1:d}".format(len(json['collections']), community[0]))
    return [(col['id'], col['name'], col['numberItems']) for col in json['collections']]


def get_items(base_url, collection):
    headers = {'Accept': 'application/json'}
    params = {'expand': 'items', "limit": 2000}
    logger.debug("Getting items for collection {0:d}".format(collection[0]))
    r = requests.get(base_url + "/collections/{0:d}".format(collection[0]), headers=headers, params=params)
    json = r.json()
    logger.debug("Found {0:d} items in collection {1:d}".format(len(json['items']), collection[0]))
    return [(item['id'], item['name'], item['lastModified'], item['expand']) for item in json['items']]


def get_item_metadata(base_url, community, collection, item):
    headers = {'Accept': 'application/json'}
    expands = item[3]
    if 'metadata' not in expands:
        logger.warning("Item {0:d} does not support metadata expansion".format(item[0]))
        return None
    params = {'expand': 'metadata'}
    logger.debug("Processing item {0:d} ...".format(item[0]))
    r = requests.get(base_url + "/items/{0:d}".format(item[0]), headers=headers, params=params)
    json = r.json()
    return json['id'], json['metadata'], json['bitstreams']


def put_key(dict, key, val):
    if key in dict:
        prev = dict[key]
        if isinstance(prev, list):
            prev.append(val)
        else:
            dict[key] = [prev, val]
    else:
        dict[key] = val


def map_item(metadata):
    meta = metadata[1]
    json = {}
    json['document_type'] = 'Publication'
    json['id'] = str(uuid.uuid4())
    for item in meta:
        key = item['key']
        val = item['value']
        if key == 'dc.contributor.author':
            put_key(json, 'creator', val)
        elif key == 'dc.identifier.citation' or key == 'dc.identifier.uri' or key == 'cg.identifier.doi' \
                or key == 'cg.identifier.url':
            put_key(json, 'identifier', val)
        elif key == 'dc.description.abstract':
            put_key(json, 'description', val)
        elif key == 'dc.language.iso':
            put_key(json, 'language', val)
        elif key == 'dc.title':
            put_key(json, 'title', val)
        elif key == 'dc.type':
            put_key(json, 'type', val)
        elif key.startswith('cg.subject') or key == 'dc.subject':
            put_key(json, 'subject', val)
        elif key.startswith('cg.contributor') or key == 'dc.description.sponsorship':
            put_key(json, 'contributor_person', val)
        elif key == 'dc.publisher':
            put_key(json, 'publisher', val)
        elif key == 'dc.source':
            put_key(json, 'source', val)
        elif key == 'dc.relation':
            put_key(json, 'relation', val)
        elif key == 'cg.coverage.region':
            put_key(json, 'coverage_region', val)
        elif key == 'cg.coverage.country':
            put_key(json, 'coverage_country', val)
        else:
            logger.warning("Element not mapped: {0:s}".format(key))
    return json


def push_to_solr(solr_url, solr_core, batch):
    logger.debug("Sending data to Solr ...")
    headers = {"Content-Type": "application/json; charset=UTF-8"}
    params = {'commit' : 'true'}
    data_str = "{" + ",".join(['"add":' + doc for doc in batch]) + "}"
    r = requests.post(solr_url + "/" + solr_core + "/update/json", params=params, headers=headers, data=data_str)
    logger.debug("<<Solr Response>>:" + r.text)
    r.raise_for_status()


def send_solr_commit(solr_url, solr_core):
    logger.info("Sending commit to Solr")
    headers = {"Content-Type": "application/json; charset=UTF-8"}
    try:
        payload = {"commit": {}}
        r = requests.post(solr_url + "/" + solr_core + "/update/json", headers=headers, data=json.dumps(payload))
    except Exception as e:
        logger.fatal("Error sending commit to Solr")
        print(e)


def add_document_to_batch(batch, doc):
    batch.append(json.dumps({"doc": doc}))


def perform_crawl(base_url, solr_url, solr_core):
    batch = []
    logger.debug("Starting crawl with {0:s} as base URL, {1:s} as Solr base and {2:s} as Solr core".format(base_url, solr_url, solr_core))
    item_count = 0
    communities = get_communities(base_url)
    for community in communities:
        collections = get_collections(base_url, community)
        for collection in collections:
            items = get_items(base_url, collection)
            for item in items:
                item_count += 1
                metadata = get_item_metadata(base_url, community, collection, item)
                json_for_solr = map_item(metadata)
                add_document_to_batch(batch, json_for_solr)
            push_to_solr(solr_url, solr_core, batch)
            batch = []
    logger.debug("Processed {0:d} items.".format(item_count))


if __name__ == '__main__':
    if len(sys.argv) == 4:
        (base_url, solr_url, solr_core) = sys.argv[1:]
        perform_crawl(base_url, solr_url, solr_core)
