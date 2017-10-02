from pyramid.config import Configurator
from pyramid.session import SignedCookieSessionFactory

import os
#Jinja2 Extensions
from jinja_extensions import setLoader, jinjaEnv

def main(global_config, **settings):
    """ This function returns a Pyramid WSGI application.
    """

    my_session_factory = SignedCookieSessionFactory('ccafsoss')

    config = Configurator(settings=settings)
    config.set_session_factory(my_session_factory)
    config.include('pyramid_jinja2')
    config.include('pyramid_fanstatic')
    config.add_static_view('static', 'static', cache_max_age=3600)
    #config.add_route('home', '/')
    config.add_route('search', '/')
    config.add_route('get', '/get/{type}')


    templatesPath = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'templates')

    # Set Jinja2 Template path
    config.add_jinja2_search_path(templatesPath)

    config.scan()

    jinjaEnv = config.get_jinja2_environment()
    setLoader(templatesPath)

    return config.make_wsgi_app()
