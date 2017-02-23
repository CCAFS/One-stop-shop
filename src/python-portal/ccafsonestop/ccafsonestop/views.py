# coding: utf-8

from pyramid.view import view_config
from resources import metroThemeLayout,metroLayoutJS
from solr import solrConnection
from pyramid.httpexceptions import HTTPFound
from webhelpers import paginate
from bs4 import BeautifulSoup
from webhelpers.html import literal
import json, pprint
from markupsafe import Markup

#@view_config(route_name='home', renderer='templates/mytemplate.jinja2')
#def my_view(request):
#    basicCSS.need()
#    return {'project': 'CCAFS One-Stop Shop'}

def stringToHex(data):
    return "".join("{:02x}".format(ord(c)) for c in data)

@view_config(route_name='search', renderer='templates/search.jinja2')
def search_view(request):

    # The POST method only collects the whether the user search for something or clicked on facted.
    # What is collected is then stored in the variable called params and then redirect to the same
    # URL but using GET
    if (request.method == 'POST'):
        params = {}

        if request.POST.get('searchtext', '') != '':
            params["q"] = request.POST.get('searchtext', '')

        if 'activefacets' in request.POST:
            if request.POST.get('activefacets', '') != '':
                activefacets = request.POST.get('activefacets', '')
                remainfacets = []
                if 'remove' in request.POST:
                    afapart = activefacets.split(" AND ")
                    facetRemoved = request.POST.get('remove', '')
                    facetRemoved = facetRemoved.replace('Document Type [','document_type:')
                    facetRemoved = facetRemoved.replace('Subject [', 'subject_raw:')
                    facetRemoved = facetRemoved.replace('Region [', 'coverage_region_raw:')
                    facetRemoved = facetRemoved.replace('Country [', 'coverage_country_raw:')
                    facetRemoved = facetRemoved.replace(']', '')
                    facetRemoved = facetRemoved.strip()
                    for aFacet in afapart:
                        if aFacet != facetRemoved:
                            remainfacets.append(aFacet)
                    activefacets = " AND ".join(remainfacets)
                if  activefacets != '':
                    params["activefacets"] = activefacets
            else:
                activefacets = ''
        else:
            activefacets = ''

        if 'fdocuments' in request.POST:
            documentFacet = 'document_type:"'+request.POST.get('fdocuments', '') + '"'
            if activefacets.find(documentFacet) == -1:
                params["fdocuments"] = documentFacet

        if 'fsubjects' in request.POST:
            subjectFacet = 'subject_raw:"' + request.POST.get('fsubjects', '') + '"'
            if activefacets.find(subjectFacet) == -1:
                params["fsubjects"] = subjectFacet

        if 'fregions' in request.POST:
            regionFacet = 'coverage_region_raw:"' + request.POST.get('fregions', '') + '"'
            if activefacets.find(regionFacet) == -1:
                params["fregions"] = regionFacet

        if 'fcountries' in request.POST:
            countryFacet = 'coverage_country_raw:"' + request.POST.get('fcountries', '') + '"'
            if activefacets.find(countryFacet) == -1:
                params["fcountries"] = countryFacet


        loc = request.route_url('search',_query=params) #Redirects to search but as GET with the collected params
        return HTTPFound(location=loc)

    if (request.method == 'GET'):

        # GET show the results of the search
        qParam = ''
        fParam = ''
        fdocuments = ''
        fsubjects = ''
        fregions = ''
        fcountries = ''

        filtered = False

        #Collect the params of the search
        if "q" in request.params:
            qParam = request.params["q"]

        if "fdocuments" in request.params:
            fdocuments =  request.params["fdocuments"]

        if "fsubjects" in request.params:
            fsubjects =  request.params["fsubjects"]

        if "fregions" in request.params:
            fregions =  request.params["fregions"]

        if "fcountries" in request.params:
            fcountries =  request.params["fcountries"]

        if "activefacets" in request.params:
            fParam =  request.params["activefacets"] + " AND "

        currentPage = 1;
        if "page" in request.params:
            currentPage =  int(request.params["page"])


        #Concatenate any facets in the params with previous facets if available
        if fdocuments != '':
            if fParam.find(fdocuments) == -1:
                fParam = fParam + fdocuments + " AND "
        if fsubjects != '':
            if fParam.find(fsubjects) == -1:
                fParam = fParam + fsubjects + " AND "
        if fregions != '':
            if fParam.find(fregions) == -1:
                fParam = fParam + fregions + " AND "
        if fcountries != '':
            if fParam.find(fcountries) == -1:
                fParam = fParam + fcountries + " AND "

        #Remove the last and
        if fParam != '':
            fParam = fParam[:-5]

        #Convert the facets from Sorl notation to readble notation
        facetArray = []
        tfacetArray = fParam.split(' AND ')
        for aFacet in tfacetArray:
            itemsections = aFacet.split(':')
            if itemsections[0] == 'document_type':
                facetArray.append('Document Type [' + itemsections[1] + '] ')
            if itemsections[0] == 'subject_raw':
                facetArray.append('Subject [' + itemsections[1] + '] ')
            if itemsections[0] == 'coverage_region_raw':
                facetArray.append('Region [' + itemsections[1] + '] ')
            if itemsections[0] == 'coverage_country_raw':
                facetArray.append('Country [' + itemsections[1] + '] ')

        #If there are not query then use all records
        if qParam == '':
            qParam = '*:*'

        #Fist call to solr is used to get the total number of hits used for pagination
        if fParam != "":
            results = solrConnection.search(qParam,**{
                'facet': 'true',
                'facet.field' : ['document_type','subject_raw','coverage_region_raw','coverage_country_raw'],
                'start': 0,
                'rows': 1,
                'sort': 'last_modified desc',
                'fq' : fParam,
            })
        else:
            results = solrConnection.search(qParam, **{
                'facet': 'true',
                'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                'start': 0,
                'sort': 'last_modified desc',
                'rows': 1,
            })

        #Creates a collection of the total amount of hits
        itemCollection = range(results.hits)
        hits = results.hits;

        #Use the pagine library to control the pagination
        page_url = paginate.PageURL_WebOb(request)
        aPage = paginate.Page(itemCollection, page=currentPage, items_per_page=7, url=page_url)


        #The second call to Solr use the pagination firtst item to pull the neccesary items of a page
        if fParam != "":
            results = solrConnection.search(qParam,**{
                'facet': 'true',
                'facet.field' : ['document_type','subject_raw','coverage_region_raw','coverage_country_raw'],
                'start': aPage.first_item-1,
                'rows': 7,
                'sort': 'last_modified desc',
                'fq' : fParam,
            })
            filtered = True
        else:
            results = solrConnection.search(qParam, **{
                'facet': 'true',
                'facet.field': ['document_type', 'subject_raw', 'coverage_region_raw', 'coverage_country_raw'],
                'start': aPage.first_item-1,
                'sort': 'last_modified desc',
                'rows': 7,
            })

        #If the no query is given then changhe qparam to nothing
        if qParam == '*:*':
            qParam = ''
        else:
            filtered = True


        #This get the different pages based on the query.
        #We modify a little the HTML given by paginate to fit the template
        pageArray = []
        tempArray = aPage.pager(separator='|||',link_attr={}).split('|||')
        for tpage in tempArray:
            soup = BeautifulSoup(str(tpage),"html.parser")
            a_tag = soup.a
            if a_tag != None:
                pageArray.append(literal('<li>' + str(a_tag) + '</li>'))
            else:
                span_tag = soup.find('span')
                if tpage.find('class="pager_curpage"') >= 0:
                    pageArray.append(literal('<li class="page-active"><a>' + str(span_tag.string) + '</a></li>'))
                if tpage.find('class="pager_dotdot"') >= 0:
                    pageArray.append(literal('<li><a>' + str(span_tag.string) + '</a></li>'))


        #Solr returns the statistics on facets using its own notation.
        #Here we change the notation to be readble and nice for the user
        regions = []
        documents = []
        subjects = []
        countries = []
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
                            regions.append({"name": itemname, "nitems": nitems})
                    if nitem%2 != 0:
                        itemname = value
                        insert = False
                    else:
                        nitems = value
                        insert = True
                if nitems > 0:
                    regions.append({"name": itemname, "nitems": nitems})
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
                    if nitem%2 != 0:
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
                    if nitem%2 != 0:
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
                    if nitem%2 != 0:
                        itemname = value
                        insert = False
                    else:
                        nitems = value
                        insert = True
                if nitems > 0:
                    countries.append({"name": itemname, "nitems": nitems})

        #pprint.pprint("**********************************")
        #pprint.pprint(results.raw_response)
        #pprint.pprint("**********************************")

        #Fixing some of the data so it can pass safely to the jinja2 template
        for result in results:
            if "thumbnail_url" not in result.keys():
                result["thumbnail_url"] = request.static_url('ccafsonestop:static/paper.png')
            if "description" in result.keys():
                pos = 0
                for adesc in result["description"]:
                    adesc = ' '.join(adesc.splitlines())
                    result["description"][pos] = adesc
                    pos = pos +1
            if "title" in result.keys():
                pos = 0
                for atitle in result["title"]:
                    atitle = ' '.join(atitle.splitlines())
                    result["title"][pos] = atitle
                    pos = pos + 1

            if "identifier" in result.keys():
                otherIdentifiers = []
                for anid in result["identifier"]:
                    if anid.find("handle") >= 0:
                        result["url"] = anid
                    else:
                        otherIdentifiers.append(anid)
                result["identifier"] = otherIdentifiers

            #Convert all the result into JSON and stored it as part of the results
            #This makes it easier to then pass it to javaScript (showresults.js)
            jsonString = json.dumps(result, ensure_ascii=False, encoding='utf-8')

            #Clearing the json from breaking characters
            jsonString = jsonString.replace("'","")
            jsonString = jsonString.replace(u'’', "'")
            jsonString = jsonString.replace(u'‐',"-")
            jsonString = jsonString.replace(u'â€˜',"")
            jsonString = jsonString.replace(u'•',"*")
            jsonString = jsonString.replace(u'—',"-")
            jsonString = jsonString.replace(u"–","-")
            #jsonString = jsonString.replace("\t", "")
            #jsonString = jsonString.replace("|", "")

            #pprint.pprint("**********************************")
            #pprint.pprint(jsonString)
            #pprint.pprint("**********************************")

            #Convert the json to Hexadecimal characteres so is embedable in HTML
            result["jsonData"] = stringToHex(jsonString)

        metroThemeLayout.need() #Injects all neccesary CSS
        metroLayoutJS.need() #Injects all neccesary js
        return {'documents': documents,'subjects':subjects,'regions':regions,'countries':countries,'qparam':qParam,'fparam':fParam,'results':results,'facetArray':facetArray,'hits':hits,'pagearray':pageArray,'filtered':filtered}
