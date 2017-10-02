from fanstatic import Library
from fanstatic import Resource
from fanstatic import Group

library = Library('ccafsonestop', 'resources')

basicCSSArray = []
basicCSSArray.append(Resource(library, 'css/bootstrap.min.css'))
basicCSSArray.append(Resource(library, 'font-awesome/css/font-awesome.css'))
basicCSSArray.append(Resource(library, 'css/style.css'))
basicCSSArray.append(Resource(library, 'css/slick/slick.css'))
basicCSSArray.append(Resource(library, 'css/slick/slick-theme.css'))
basicCSSArray.append(Resource(library, 'css/select2/select2.css'))


basicCSS = Group(basicCSSArray)

search = Resource(library, 'css/search.css',depends=[basicCSS])

JQuery = Resource(library, 'js/jquery-3.1.1.min.js',bottom=False)
circles = Resource(library, 'js/circles.min.js',depends=[JQuery],bottom=False)
bootStrap = Resource(library, 'js/bootstrap.min.js',depends=[circles],bottom=False)
slick = Resource(library, 'js/slick/slick.min.js',depends=[bootStrap],bottom=False)
ias = Resource(library, 'js/jquery-ias.min.js',depends=[slick],bottom=False)
select2 = Resource(library, 'js/select2/select2.js',depends=[slick],bottom=False)


ccafs = Resource(library, 'js/ccafs.js',depends=[select2],bottom=True)