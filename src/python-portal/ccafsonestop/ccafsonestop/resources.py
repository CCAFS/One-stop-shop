from fanstatic import Library
from fanstatic import Resource
from fanstatic import Group

library = Library('ccafsonestop', 'resources')

basicCSSArray = []
basicCSSArray.append(Resource(library, 'bootstrap.min.css'))
basicCSSArray.append(Resource(library, 'theme.css'))
basicCSS = Group(basicCSSArray)

metroMandatoryCSSArray = []
metroMandatoryCSSArray.append(Resource(library, 'assets/global/plugins/font-awesome/css/font-awesome.min.css'))
metroMandatoryCSSArray.append(Resource(library, 'assets/global/plugins/simple-line-icons/simple-line-icons.min.css'))
metroMandatoryCSSArray.append(Resource(library, 'assets/global/plugins/bootstrap/css/bootstrap.min.css'))
metroMandatoryCSSArray.append(Resource(library, 'assets/global/plugins/bootstrap-switch/css/bootstrap-switch.min.css'))
metroMandatoryCSS = Group(metroMandatoryCSSArray)

metroPageCSSArray = []
metroPageCSSArray.append(Resource(library, 'assets/global/plugins/bootstrap-datepicker/css/bootstrap-datepicker3.min.css',depends=[metroMandatoryCSS]))
metroPageCSSArray.append(Resource(library, 'assets/global/plugins/fancybox/source/jquery.fancybox.css',depends=[metroMandatoryCSS]))
metroPageCSS = Group(metroPageCSSArray)

metroGlobalStyleCSSArray = []
metroGlobalStyleCSSArray.append(Resource(library, 'assets/global/css/components.min.css',depends=[metroPageCSS]))
metroGlobalStyleCSSArray.append(Resource(library, 'assets/global/css/plugins.min.css',depends=[metroPageCSS]))
metroGlobalStyleCSS = Group(metroGlobalStyleCSSArray)

metroSearchCSSArray = []
metroSearchCSSArray.append(Resource(library, 'assets/pages/css/search.min.css',depends=[metroGlobalStyleCSS]))
metroSearchCSSArray.append(Resource(library, 'assets/pages/css/profile.min.css',depends=[metroGlobalStyleCSS]))
metroSearchCSSArray.append(Resource(library, 'ccafsoss.css',depends=[metroGlobalStyleCSS]))
metroSearchCSS = Group(metroSearchCSSArray)

metroThemeLayoutArray =[]
metroThemeLayoutArray.append(Resource(library, 'assets/layouts/layout/css/layout.min.css',depends=[metroSearchCSS]))
metroThemeLayoutArray.append(Resource(library, 'assets/layouts/layout/css/themes/darkblue.min.css',depends=[metroSearchCSS]))
metroThemeLayoutArray.append(Resource(library, 'assets/layouts/layout/css/custom.min.css',depends=[metroSearchCSS]))
metroThemeLayout = Group(metroThemeLayoutArray)

JQuery = Resource(library, 'assets/global/plugins/jquery.min.js',bottom=True)
bootStrap = Resource(library, 'assets/global/plugins/bootstrap/js/bootstrap.min.js',depends=[JQuery],bottom=True)

metroCoreJSArray = []
metroCoreJSArray.append(Resource(library, 'assets/global/plugins/js.cookie.min.js',depends=[bootStrap],bottom=True))
metroCoreJSArray.append(Resource(library, 'assets/global/plugins/jquery-slimscroll/jquery.slimscroll.min.js',depends=[bootStrap],bottom=True))
metroCoreJSArray.append(Resource(library, 'assets/global/plugins/jquery.blockui.min.js',depends=[bootStrap],bottom=True))
metroCoreJSArray.append(Resource(library, 'assets/global/plugins/bootstrap-switch/js/bootstrap-switch.min.js',depends=[bootStrap],bottom=True))
metroCoreJS = Group(metroCoreJSArray)

metroPageJSArray = []
metroPageJSArray.append(Resource(library, 'assets/global/plugins/bootstrap-datepicker/js/bootstrap-datepicker.min.js',depends=[metroCoreJS],bottom=True))
metroPageJSArray.append(Resource(library, 'assets/global/plugins/fancybox/source/jquery.fancybox.pack.js',depends=[metroCoreJS],bottom=True))
metroPageJS = Group(metroPageJSArray)

metroGlobalStyleJS = Resource(library, 'assets/global/scripts/app.min.js',depends=[metroPageJS],bottom=True)

searchJSArray =[]
searchJSArray.append(Resource(library, 'assets/pages/scripts/search.min.js',depends=[metroGlobalStyleJS],bottom=True))
searchJSArray.append(Resource(library, 'assets/pages/scripts/profile.min.js',depends=[metroGlobalStyleJS],bottom=True))
searchJS = Group(searchJSArray)

metroLayoutJSArray = []
metroLayoutJSArray.append(Resource(library, 'assets/layouts/layout/scripts/layout.min.js',depends=[searchJS],bottom=True))
metroLayoutJSArray.append(Resource(library, 'assets/layouts/layout/scripts/demo.min.js',depends=[searchJS],bottom=True))
metroLayoutJSArray.append(Resource(library, 'assets/layouts/global/scripts/quick-sidebar.min.js',depends=[searchJS],bottom=True))
metroLayoutJSArray.append(Resource(library, 'assets/layouts/global/scripts/quick-nav.min.js',depends=[searchJS],bottom=True))

metroLayoutJSArray.append(Resource(library, 'showresult.js',depends=[searchJS],bottom=True))

metroLayoutJS = Group(metroLayoutJSArray)