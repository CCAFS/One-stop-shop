# coding: utf-8

from pyramid.view import view_config
from solr import solrConnection
from pyramid.httpexceptions import HTTPFound
from webhelpers import paginate
import json
from resources import bootStrap,ccafs,search



#@view_config(route_name='home', renderer='templates/mytemplate.jinja2')
#def my_view(request):
#    basicCSS.need()
#    return {'project': 'CCAFS One-Stop Shop'}

def stringToHex(data):
    return "".join("{:02x}".format(ord(c)) for c in data)

def getDocuments():
    documents = []
    results = solrConnection.search('*:*', **{
        'facet': 'true',
        'facet.field': ['document_type'],
        'start': 0,
        'sort': 'last_modified desc',
        'rows': 1,
    })
    for key, values in results.facets['facet_fields'].iteritems():
        if key == 'document_type':
            nitem = 0
            insert = False
            itemname = ''
            nitems = 0
            for value in values:
                nitem = nitem + 1
                if insert:
                    if nitems > 0:
                        documents.append({"name": itemname, "nitems": nitems})
                if nitem % 2 != 0:
                    itemname = value
                    insert = False
                else:
                    nitems = value
                    insert = True
            if nitems > 0:
                documents.append({"name": itemname, "nitems": nitems})
    return documents

def getFields():
    fields = []
    fields.append({'name': 'title', 'desc': 'Title'})
    fields.append({'name': 'creator', 'desc': 'Creator'})
    fields.append({'name': 'description', 'desc': 'Abstract'})
    fields.append({'name': 'publisher', 'desc': 'Publisher'})
    fields.append({'name': 'contributor_person', 'desc': 'Contributing person'})
    fields.append({'name': 'contributor_organization', 'desc': 'Contributing organization'})
    fields.append({'name': 'contributor_partner', 'desc': 'Contributing partner'})
    fields.append({'name': 'contributor_funder', 'desc': 'Contributing funder'})
    fields.append({'name': 'contributor_project', 'desc': 'Contributing project'})
    fields.append({'name': 'contributor_project_lead_institution', 'desc': 'Contributing project lead institution'})
    fields.append({'name': 'production_date', 'desc': 'Production date'})
    fields.append({'name': 'availability_date', 'desc': 'Availability date'})
    fields.append({'name': 'type', 'desc': 'Type'})
    fields.append({'name': 'format', 'desc': 'Format'})
    fields.append({'name': 'identifier', 'desc': 'Identifier'})
    fields.append({'name': 'source', 'desc': 'Source'})
    fields.append({'name': 'language', 'desc': 'Language'})
    fields.append({'name': 'relation', 'desc': 'Relation'})
    fields.append({'name': 'coverage_admin_unit', 'desc': 'Coverage administrative unit'})
    fields.append({'name': 'rights', 'desc': 'Rights'})
    fields.append({'name': 'contact', 'desc': 'Contact'})
    return fields

def getFieldDesc(fieldName):
    fields = getFields()
    for field in fields:
        if field["name"] == fieldName:
            return field["desc"]
    return ""

def fieldExists(code):
    fields = getFields()
    for field in fields:
        if field["name"] == code:
            return True
    return False

def getTotal(freeSearch):
    results = solrConnection.search(freeSearch, **{
        'facet': 'true',
        'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
        'start': 0,
        'sort': 'last_modified desc',
        'rows': 1,
    })
    return results.hits

@view_config(route_name='get', renderer='json')
def get_view(request):
    sourceType = request.matchdict['type']
    sourceType = sourceType.lower()
    regions = []
    documents = []
    subjects = []
    countries = []
    crps = []
    centres = []

    results = solrConnection.search('*:*', **{
        'facet': 'true',
        'facet.field': ['document_type', 'subject_raw',
                        'coverage_region_raw', 'coverage_country_raw'],#'facet.field': ['document_type', 'subject_raw', 'contributor_crp_raw ','contributor_center_raw ', 'coverage_region_raw', 'coverage_country_raw'],
        'start': 0,
        'sort': 'last_modified desc',
        'rows': 1,
    })
    for key, values in results.facets['facet_fields'].iteritems():
        if key == 'coverage_region_raw':
            nitem = 0
            insert = False
            itemname = ''
            nitems = 0
            for value in values:
                nitem = nitem + 1
                if insert:
                    if nitems > 0:
                        regions.append({'name': itemname, 'nitems': nitems})
                if nitem % 2 != 0:
                    itemname = value
                    insert = False
                else:
                    nitems = value
                    insert = True
            if nitems > 0:
                regions.append({'name': itemname, 'nitems': nitems})
        if key == 'document_type':
            nitem = 0
            insert = False
            itemname = ''
            nitems = 0
            for value in values:
                nitem = nitem + 1
                if insert:
                    if nitems > 0:
                        documents.append({"name": itemname, "nitems": nitems})
                if nitem % 2 != 0:
                    itemname = value
                    insert = False
                else:
                    nitems = value
                    insert = True
            if nitems > 0:
                documents.append({"name": itemname, "nitems": nitems})
        if key == 'subject_raw':
            nitem = 0
            insert = False
            itemname = ''
            nitems = 0
            for value in values:
                nitem = nitem + 1
                if insert:
                    if nitems > 0:
                        subjects.append({"name": itemname, "nitems": nitems})
                if nitem % 2 != 0:
                    itemname = value
                    insert = False
                else:
                    nitems = value
                    insert = True
            if nitems > 0:
                subjects.append({"name": itemname, "nitems": nitems})
        if key == 'coverage_country_raw':
            nitem = 0
            insert = False
            itemname = ''
            nitems = 0
            for value in values:
                nitem = nitem + 1
                if insert:
                    if nitems > 0:
                        countries.append({"name": itemname, "nitems": nitems})
                if nitem % 2 != 0:
                    itemname = value
                    insert = False
                else:
                    nitems = value
                    insert = True
            if nitems > 0:
                countries.append({"name": itemname, "nitems": nitems})

        try:
            if key == 'contributor_crp_raw':
                nitem = 0
                insert = False
                itemname = ''
                nitems = 0
                for value in values:
                    nitem = nitem + 1
                    if insert:
                        if nitems > 0:
                            crps.append({"name": itemname, "nitems": nitems})
                    if nitem % 2 != 0:
                        itemname = value
                        insert = False
                    else:
                        nitems = value
                        insert = True
                if nitems > 0:
                    crps.append({"name": itemname, "nitems": nitems})
        except:
            pass

        try:
            if key == 'contributor_center_raw':
                nitem = 0
                insert = False
                itemname = ''
                nitems = 0
                for value in values:
                    nitem = nitem + 1
                    if insert:
                        if nitems > 0:
                            centres.append({"name": itemname, "nitems": nitems})
                    if nitem % 2 != 0:
                        itemname = value
                        insert = False
                    else:
                        nitems = value
                        insert = True
                if nitems > 0:
                    centres.append({"name": itemname, "nitems": nitems})
        except:
            pass



    if sourceType == 'type':
        return documents

    if sourceType == 'subject':
        return subjects

    if sourceType == 'region':
        return regions

    if sourceType == 'country':
        return countries

    if sourceType == 'crps':
        return crps

    if sourceType == 'centres':
        return centres

    return {}

def getFacetCode(facetName):
    if facetName == 'Type':
        return 'document_type'
    if facetName == 'Subject':
        return 'subject_raw'
    if facetName == 'CRP':
        return 'contributor_crp_raw'
    if facetName == 'Centre':
        return 'contributor_center_raw'
    if facetName == 'Region':
        return 'coverage_region_raw'
    if facetName == 'Country':
        return 'coverage_country_raw'

def getFacetName(facetCode):
    if facetCode == 'document_type':
        return 'Type'
    if facetCode == 'subject_raw':
        return 'Subject'
    if facetCode == 'contributor_crp_raw':
        return 'CRP'
    if facetCode == 'contributor_center_raw':
        return 'Centre'
    if facetCode == 'coverage_region_raw':
        return 'Region'
    if facetCode == 'coverage_country_raw':
        return 'Country'

def getFacetDesc(facetCode):
    if facetCode == 'document_type':
        return 'Document type'
    if facetCode == 'subject_raw':
        return 'Subject'
    if facetCode == 'contributor_crp_raw':
        return 'CRP'
    if facetCode == 'contributor_center_raw':
        return 'Centre'
    if facetCode == 'coverage_region_raw':
        return 'Region'
    if facetCode == 'coverage_country_raw':
        return 'Country'

def getJoinDesc(code):
    if code == "and":
        return "Contains"
    if code == "not":
        return "Exclude"
    return code

@view_config(route_name='search', renderer='templates/search2.jinja2')
def search_view(request):

    # The POST method only collects the whether the user search for something or clicked on facted.
    # What is collected is then stored in the variable called params and then redirect to the same
    # URL but using GET
    if (request.method == 'POST'):
        params = {}

        if 'q' in request.params.keys():
            params["q"] = request.params["q"]

        if request.POST.get('searchtext', '') != '':
            params["q"] = request.POST.get('searchtext', '')
        else:
            params["q"] = ""

        if params["q"] == "":
            params.pop('q', None)



        activeFacets = []
        #Obtains facets in the URL
        for key in request.params.keys():
            if key.find("facet_") >= 0:
                activeFacets.append({'facet': key.replace("facet_", ''), 'value': request.params[key]})

        if 'removequery' in request.POST:
            params.pop('q', None)

        #if we adding a filtering by a facet
        if 'facetadd' in request.POST:
            facetType = getFacetCode(request.POST.get('facettype', ''))
            facetValue = request.POST.get('facetvalue', '')
            facetFound = False
            for facet in activeFacets:
                if facet["facet"] == facetType:
                    facetFound = True
            if not facetFound:
                activeFacets.append({'facet': facetType, 'value': facetValue})


        # if we removing a filtering by a facet
        if 'facetremove' in request.POST:
            facetRemoved = request.POST.get('facetremove', '')
            newFacets = filter(lambda x: x["facet"] != facetRemoved, activeFacets)
            activeFacets = newFacets

        activeFields = []
        for key in request.params.keys():
            if key.find("fields") >= 0:
                tfieldValue = request.params[key]
                fieldArray = tfieldValue.split("+")
                for field in fieldArray:
                    try:
                        fieldCode, fieldValue, fieldJoin = field.split("|")
                        activeFields.append({'field': fieldCode, 'value': fieldValue,'join':fieldJoin})
                    except:
                        pass

        # if we adding a field search
        if 'fieldadd' in request.POST:
            fieldType = request.POST.get('fieldtype', 'NA')
            if fieldType != "NA":
                fieldValue = request.POST.get('fieldvalue', '')
                if fieldValue != "":
                    fieldJoin = request.POST.get('fieldjoin', 'NA')
                    if fieldJoin != "NA":
                        fieldFound = False
                        for field in activeFields:
                            if field["field"] == fieldType and field["value"] == fieldValue:
                                fieldFound = True
                        if not fieldFound:
                            activeFields.append({'field': fieldType, 'value': fieldValue, 'join': fieldJoin})

        # if we removing a field search
        if 'fieldremove' in request.POST:
            try:
                fieldType,fieldValue = request.POST.get('fieldremove', '').split("|")
                remainFields = []
                for field in activeFields:
                    if not (field["field"] == fieldType and field["value"] == fieldValue) :
                        remainFields.append(field)
                activeFields = remainFields
            except:
                pass


        #Add the active fields to params
        fieldArray = []
        for field in activeFields:
            fieldArray.append(field["field"] + "|" + field["value"] + "|" + field["join"])

        params["fields"] = "+".join(fieldArray)

        # Add active facets to params
        for facet in activeFacets:
            params["facet_" + facet["facet"]] = facet["value"]

        loc = request.route_url('search', _query=params)  # Redirects to search but as GET with the collected params
        return HTTPFound(location=loc)


    else:
        #ge go through the params in the request to see what is the user searching for

        if not 'q' in request.params.keys():
            qParam = ''
        else:
            qParam = request.params["q"]

        activeFacets = []
        for key in request.params.keys():
            if key.find("facet_") >= 0:
                activeFacets.append({'code':key.replace("facet_",''),'value':request.params[key],'name':getFacetName(key.replace("facet_",'')),'desc':getFacetDesc(key.replace("facet_",''))})

        activeFields = []
        for key in request.params.keys():
            if key.find("fields") >= 0:
                tfieldValue = request.params[key]
                fieldArray = tfieldValue.split("+")
                for field in fieldArray:
                    try:
                        fieldCode, fieldValue, fieldJoin = field.split("|")
                        if fieldExists(fieldCode):
                            if fieldValue != "":
                                if fieldJoin == "and" or fieldJoin == "or" or fieldJoin == "not":
                                    activeFields.append({'code': fieldCode, 'value': fieldValue, 'join': fieldJoin,'joindesc':getJoinDesc(fieldJoin),'name':getFieldDesc(fieldCode)})
                    except:
                        pass

        currentPage = 1
        if "page" in request.params:
            currentPage = int(request.params["page"])

        #-------------------------------------------------------------------------------------------------------
        #Now we search solr using what the user indicated

        if qParam == "":
            freeSearch = "*:*"
        else:
            freeSearch = qParam

        tFieldArray = []
        for field in activeFields:
            sField = ""
            if field["join"] == "and":
                sField = "+"
            if field["join"] == "not":
                sField = "-"
            sField = sField + field["code"] + ':"' + field["value"] + '"'
            tFieldArray.append(sField)

        fieldString = " ".join(tFieldArray)
        if activeFields:
            freeSearch = freeSearch + " " + fieldString


        #Get the statistics for each facet
        facetStats = []
        nstat = 0
        if activeFacets:
            for aFacet in activeFacets:
                facetString = aFacet["code"] + ":" + '"' + aFacet["value"] + '"'
                results = solrConnection.search("*:*", **{
                    'facet': 'true',
                    'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                    'start': 0,
                    'rows': 1,
                    'sort': 'last_modified desc',
                    'fq': facetString,
                })
                nstat = nstat + 1
                facetStats.append({'nstat': nstat, 'name': aFacet["name"],'value':aFacet["value"], 'total': results.hits})

        # Get the statistics for each field
        fieldStats = []
        nstat = 0
        if activeFields:
            for aField in activeFields:
                if aField["join"] != "not":
                    fieldString = "+" + aField["code"] + ":" + '"' + aField["value"] + '"'
                else:
                    fieldString = "-" + aField["code"] + ":" + '"' + aField["value"] + '"'
                results = solrConnection.search(fieldString, **{
                    'facet': 'true',
                    'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw',
                                    'coverage_country_raw'],
                    'start': 0,
                    'rows': 1,
                    'sort': 'last_modified desc',
                })
                nstat = nstat + 1
                fieldStats.append({'nstat': nstat, 'name': aField["name"],'value':aField["value"],'join':aField["join"] ,'total': results.hits})

        # Now we query the qparam if there is
        totalinqparam = 0
        if qParam != '':
            results = solrConnection.search(qParam, **{
                'facet': 'true',
                'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                'start': 0,
                'sort': 'last_modified desc',
                'rows': 1,
            })
            totalinqparam = results.hits

        #Creates an array of facets with the Solr syntax
        facetArray = []
        if activeFacets:
            for facet in activeFacets:
                facetArray.append(facet["code"] + ':"' + facet["value"] + '"')

        # Fist call to solr is used to get the total number of hits used for pagination
        if activeFacets:
            results = solrConnection.search(freeSearch, **{
                'facet': 'true',
                'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                'start': 0,
                'rows': 1,
                'sort': 'last_modified desc',
                'fq': ' AND '.join(facetArray),
            })
        else:
            results = solrConnection.search(freeSearch, **{
                'facet': 'true',
                'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                'start': 0,
                'sort': 'last_modified desc',
                'rows': 1,
            })

        hits = results.hits
        itemCollection = range(hits)
        grandTotal = getTotal("*:*")
        totalInQParam = getTotal(freeSearch)

        # Use the pagine library to control the pagination
        page_url = paginate.PageURL_WebOb(request)
        aPage = paginate.Page(itemCollection, page=currentPage, items_per_page=7, url=page_url)
        nopage = {}
        for key, value in request.params.iteritems():
            if key != "page":
                nopage[key] = value

        allPages = []
        if aPage.last_page > 1:
            for a in range(1, aPage.last_page):
                nextpage = False
                if a == currentPage + 1:
                    nextpage = True
                nopage["page"] = a
                allPages.append({"page": a, 'next': nextpage, 'url': request.route_url('search', _query=nopage)})




        if hits > 0:
            # The second call to Solr use the pagination firtst item to pull the neccesary items of a page
            if activeFacets:
                results = solrConnection.search(freeSearch, **{
                    'facet': 'true',
                    'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                    'start': aPage.first_item - 1,
                    'rows': 7,
                    'sort': 'last_modified desc',
                    'fq': ' AND '.join(facetArray),
                })
            else:
                results = solrConnection.search(freeSearch, **{
                    'facet': 'true',
                    'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                    'start': aPage.first_item - 1,
                    'sort': 'last_modified desc',
                    'rows': 7,
                })
        else:
            results = []

        for result in results:
            if "thumbnail_url" not in result.keys():
                result["thumbnail_url"] = request.static_url('ccafsonestop:static/paper.png')

            jdata = json.dumps(result)
            result["jsonData"] = jdata.decode('utf8').encode('ascii')

        search.need() #Injects all neccesary CSS
        bootStrap.need() #Injects all neccesary js
        ccafs.need()



        return {'totalinqparam':totalinqparam,'hits':hits,'qparam':qParam,
                'activeFacets': activeFacets,'doclist':getDocuments(),'facetStats':facetStats,
                'results': results,'allPages':allPages,
                'grandTotal': grandTotal,'fields':getFields(),'activeFields':activeFields,
                'totalInQParam':totalInQParam,'fieldStats':fieldStats}