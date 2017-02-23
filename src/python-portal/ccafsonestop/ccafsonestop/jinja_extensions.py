from jinja2 import nodes
from jinja2 import ext
from jinja2 import Environment
from webhelpers.html import literal
from jinja2 import FileSystemLoader
import re

jinjaEnv = Environment()

def setLoader(pathToTemplates):
    jinjaEnv.loader = FileSystemLoader(pathToTemplates)

def render_snippet(template_name, **kw):
    ''' This function will render the snippet.

    This function is based on CKAN code which is licensed as follows

    CKAN - Data Catalogue Software
    Copyright (C) 2007 Open Knowledge Foundation
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.

     '''

    template = jinjaEnv.get_template(template_name)
    output = template.render(kw,renderer='snippet')
    output = '\n<!-- Snippet %s start -->\n%s\n<!-- Snippet %s end -->\n' % (template_name, output, template_name)
    return literal(output)


class BaseExtension(ext.Extension):
    ''' Base class for creating custom jinja2 tags.
    parse expects a tag of the format
    {% tag_name args, kw %}
    after parsing it will call _call(args, kw) which must be defined.

    This class is based on CKAN code which is licensed as follows

    CKAN - Data Catalogue Software
    Copyright (C) 2007 Open Knowledge Foundation
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.


    '''

    def parse(self, parser):
        stream = parser.stream
        tag = stream.next()
        # get arguments
        args = []
        kwargs = []
        while not stream.current.test_any('block_end'):
            if args or kwargs:
                stream.expect('comma')
            if stream.current.test('name') and stream.look().test('assign'):
                key = nodes.Const(stream.next().value)
                stream.skip()
                value = parser.parse_expression()
                kwargs.append(nodes.Pair(key, value, lineno=key.lineno))
            else:
                args.append(parser.parse_expression())

        def make_call_node(*kw):
            return self.call_method('_call', args=[
                nodes.List(args),
                nodes.Dict(kwargs),
            ], kwargs=kw)

        return nodes.Output([make_call_node()]).set_lineno(tag.lineno)

class SnippetExtension(BaseExtension):
    ''' Custom snippet tag

    {% snippet <template_name> [, <keyword>=<value>].. %}

    This class is based on CKAN code which is licensed as follows

    CKAN - Data Catalogue Software
    Copyright (C) 2007 Open Knowledge Foundation
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.


    '''

    tags = set(['snippet'])

    @classmethod
    def _call(cls, args, kwargs):
        assert len(args) == 1
        return render_snippet(args[0], **kwargs)

def regularise_html(html):
    ''' Take badly formatted html with strings '''

    # This code is based on CKAN code which is licensed as follows
    #
    # CKAN - Data Catalogue Software
    # Copyright (C) 2007 Open Knowledge Foundation
    # This program is free software: you can redistribute it and/or modify
    # it under the terms of the GNU Affero General Public License as
    # published by the Free Software Foundation, either version 3 of the
    # License, or (at your option) any later version.
    # This program is distributed in the hope that it will be useful,
    # but WITHOUT ANY WARRANTY; without even the implied warranty of
    # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    # GNU Affero General Public License for more details.
    # You should have received a copy of the GNU Affero General Public License
    # along with this program. If not, see <http://www.gnu.org/licenses/>.


    if html is None:
        return
    html = re.sub('\n', ' ', html)
    matches = re.findall('(<[^>]*>|%[^%]\([^)]*\)\w|[^<%]+|%)', html)
    for i in xrange(len(matches)):
        match = matches[i]
        if match.startswith('<') or match.startswith('%'):
            continue
        matches[i] = re.sub('\s{2,}', ' ', match)
    html = ''.join(matches)
    return html
