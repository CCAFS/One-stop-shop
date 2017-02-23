
/*
    This function show the details of a document in the modal window
 */

function hex2a(hexx) {
    var hex = hexx.toString();//force conversion
    var str = '';
    for (var i = 0; i < hex.length; i += 2)
        str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    return str;
}


function showDocumentDetails(hexdata)
{
    var data = hex2a(hexdata);
    //alert(decodeURIComponent((data)));

    //alert(data);
    var obj = jQuery.parseJSON(data);
    jQuery("#docimage").attr('src',obj.thumbnail_url);

    if (obj.hasOwnProperty('title'))
        $("#doctitle").text(obj.title);
    else
        $("#doctitle").text("Without title");

    if (obj.hasOwnProperty('url')) {
        $("#doc_url").text(obj.url);
        $("#doc_url2").attr('href', obj.url);
        $("#doc_url2").attr('target', "_blank");
    }
    else {
        $("#doc_url").text("Without url");
        $("#doc_url2").attr('href', "");
        $("#doc_url2").attr('target', "");
    }
    if (obj.hasOwnProperty('description'))
        $("#doc_description").text(obj.description);
    else
        $("#doc_description").text("Without description");

    if (obj.hasOwnProperty('subject'))
        $("#doc_subject").text(obj.subject);
    else
        $("#doc_subject").text("Without subjects");

    if (obj.hasOwnProperty('creator'))
        $("#doc_creator").text(obj.creator);
    else
        $("#doc_creator").text("Without authors");

    if (obj.hasOwnProperty('publisher'))
        $("#doc_publisher").text(obj.publisher);
    else
        $("#doc_publisher").text("Without publisher.");

    var contributors = "";
    if (obj.hasOwnProperty('contributor_person'))
        contributors = contributors + obj.contributor_person;
    if (obj.hasOwnProperty('contributor_organization'))
        contributors = contributors + obj.contributor_organization;
    if (obj.hasOwnProperty('contributor_center'))
        contributors = contributors + obj.contributor_center;
    if (obj.hasOwnProperty('contributor_crp'))
        contributors = contributors + obj.contributor_crp;
    if (obj.hasOwnProperty('contributor_partner'))
        contributors = contributors + obj.contributor_partner;
    if (obj.hasOwnProperty('contributor_funder'))
        contributors = contributors + obj.contributor_funder;
    if (obj.hasOwnProperty('contributor_project'))
        contributors = contributors + obj.contributor_project;
    if (obj.hasOwnProperty('contributor_project_lead_institution'))
        contributors = contributors + obj.contributor_project_lead_institution;

    if (contributors != "")
         $("#doc_contributors").text(contributors);

    if (obj.hasOwnProperty('production_date'))
        $("#doc_production_date").text(obj.production_date);
    else
        $("#doc_production_date").text("Without production date.");

    if (obj.hasOwnProperty('distribution_date'))
        $("#doc_distribution_date").text(obj.distribution_date);
    else
        $("#doc_distribution_date").text("Without distribution date.");

    if (obj.hasOwnProperty('type'))
        $("#doc_type").text(obj.type);
    else
        $("#doc_type").text("Without type.");

    if (obj.hasOwnProperty('format'))
        $("#doc_format").text(obj.format);
    else
        $("#doc_format").text("Without format.");

    if (obj.hasOwnProperty('identifier'))
        $("#doc_identifier").text(obj.identifier);
    else
        $("#doc_identifier").text("Without identifier.");

    if (obj.hasOwnProperty('source'))
        $("#doc_source").text(obj.source);
    else
        $("#doc_source").text("Without source.");

    if (obj.hasOwnProperty('language'))
        $("#doc_language").text(obj.language);
    else
        $("#doc_language").text("Without language.");

    if (obj.hasOwnProperty('relation'))
        $("#doc_relation").text(obj.relation);
    else
        $("#doc_relation").text("Without relations.");

    if (obj.hasOwnProperty('coverage_region'))
        $("#doc_coverage_region").text(obj.coverage_region);
    else
        $("#doc_coverage_region").text("Without regions.");

    if (obj.hasOwnProperty('coverage_country'))
        $("#doc_coverage_country").text(obj.coverage_country);
    else
        $("#doc_coverage_country").text("Without countries.");

    if (obj.hasOwnProperty('coverage_admin_unit'))
        $("#doc_coverage_admin_unit").text(obj.coverage_admin_unit);
    else
        $("#doc_coverage_admin_unit").text("Without administrative units.");

    if (obj.hasOwnProperty('rights'))
        $("#doc_rights").text(obj.rights);
    else
        $("#doc_rights").text("Without information about rights.");

    if (obj.hasOwnProperty('contact'))
        $("#doc_contact").text(obj.contact);
    else
        $("#doc_contact").text("Without contact information.");


    $('#large').modal('show');
    setTimeout( function() {$("#cgcore").scrollTop(0)}, 200 );
}